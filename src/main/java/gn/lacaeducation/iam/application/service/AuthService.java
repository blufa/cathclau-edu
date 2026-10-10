package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.iam.application.event.PasswordResetEvent;
import gn.lacaeducation.iam.application.event.UserLoggedInEvent;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.PasswordResetTokenRepository;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.TokenGenerator;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.PasswordResetOtp;
import gn.lacaeducation.iam.domain.PasswordResetToken;
import gn.lacaeducation.iam.domain.RefreshToken;
import gn.lacaeducation.iam.domain.User;
import gn.lacaeducation.common.TenantInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;
    private static final int RESET_TOKEN_EXPIRY_HOURS = 24;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final gn.lacaeducation.iam.application.port.out.PasswordResetOtpRepository passwordResetOtpRepository;
    private final TenantInfoProvider tenantInfoProvider;
    private final ApplicationEventPublisher eventPublisher;

    private static final int OTP_EXPIRY_MINUTES = 10;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResult authenticate(LoginCommand command) {
        // Connexion possible par username, email OU numéro de téléphone (plusieurs formats acceptés).
        User user = userRepository.findByUsername(command.identifier())
                .or(() -> userRepository.findByEmail(command.identifier()))
                .or(() -> resolveByPhone(command.identifier()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("Account disabled");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Account locked until " + user.getLockedUntil());
        }

        if (!passwordHasher.verify(command.password(), user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        // Credentials are valid: reject if the tenant is suspended (403) without leaking tenant
        // status to failed login attempts.
        requireActiveTenant(user);

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        List<String> roles = rolesOf(user.getId());
        List<String> permissions = permissionsFor(user.getId(), roles);

        String accessToken = tokenGenerator.generate(user.getId(), user.getTenantId(), roles, permissions);
        String refreshTokenValue = generateRefreshTokenForUser(user.getId());

        eventPublisher.publishEvent(new UserLoggedInEvent(user.getId(), user.getTenantId(), user.getLastLoginAt()));

        return new LoginResult(accessToken, refreshTokenValue, jwtExpiration,
                user.getId(), user.getTenantId(), roles, user.isMustChangePassword());
    }

    @Override
    public LoginResult refreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new BusinessRuleException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("Account disabled");
        }

        requireActiveTenant(user);

        List<String> roles = rolesOf(user.getId());
        List<String> permissions = permissionsFor(user.getId(), roles);

        String accessToken = tokenGenerator.generate(user.getId(), user.getTenantId(), roles, permissions);
        String newRefreshToken = generateRefreshTokenForUser(user.getId());
        refreshTokenRepository.revokeByToken(refreshTokenValue);

        return new LoginResult(accessToken, newRefreshToken, jwtExpiration,
                user.getId(), user.getTenantId(), roles, user.isMustChangePassword());
    }

    @Override
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.revokeByToken(refreshTokenValue);
    }

    @Override
    public void forgotPassword(String email) {
        // Always return success to avoid user enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            // Sans cet événement, le jeton n'existait qu'en base : l'utilisateur n'avait aucun moyen
            // de le connaître pour réinitialiser son mot de passe.
            eventPublisher.publishEvent(new gn.lacaeducation.iam.application.event.PasswordResetRequestedEvent(
                    user.getTenantId(), user.getId(), email, token));
        });
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(command.token())
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new BusinessRuleException("Reset token has already been used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Reset token has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", resetToken.getUserId()));

        user.setPasswordHash(passwordHasher.hash(command.newPassword()));
        user.setMustChangePassword(false);
        // Un reset réussi doit aussi lever un éventuel verrou (5 tentatives échouées) — sinon un compte
        // verrouillé reste bloqué malgré un nouveau mot de passe correct, le verrou expirant seulement
        // après son délai initial (30 min), sans rapport avec la réussite du reset.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        eventPublisher.publishEvent(new PasswordResetEvent(user.getId(), user.getTenantId()));
    }

    @Override
    public void requestPasswordResetOtp(String identifier) {
        // Même résolution que la connexion (username, email ou téléphone) — anti-énumération : ne
        // révèle jamais si l'identifiant existe.
        resolveIdentifier(identifier).ifPresent(user -> {
            if (user.getPhone() == null || user.getPhone().isBlank()) return;
            String code = String.format("%06d", (int) (Math.random() * 1_000_000));
            PasswordResetOtp otp = PasswordResetOtp.builder()
                    .userId(user.getId())
                    .code(code)
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .used(false)
                    .build();
            passwordResetOtpRepository.save(otp);
            eventPublisher.publishEvent(new gn.lacaeducation.iam.application.event.PasswordResetOtpRequestedEvent(
                    user.getTenantId(), user.getId(), user.getPhone(), code));
        });
    }

    @Override
    public void resetPasswordWithOtp(ResetWithOtpCommand command) {
        User user = resolveIdentifier(command.identifier())
                .orElseThrow(() -> new BusinessRuleException("Code invalide ou expiré"));
        PasswordResetOtp otp = passwordResetOtpRepository.findByUserIdAndCode(user.getId(), command.code())
                .orElseThrow(() -> new BusinessRuleException("Code invalide ou expiré"));
        if (otp.isUsed()) {
            throw new BusinessRuleException("Ce code a déjà été utilisé");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Ce code a expiré");
        }

        user.setPasswordHash(passwordHasher.hash(command.newPassword()));
        user.setMustChangePassword(false);
        // Même raison que resetPassword (token) : un reset réussi lève aussi un éventuel verrou.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        otp.setUsed(true);
        passwordResetOtpRepository.save(otp);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        eventPublisher.publishEvent(new PasswordResetEvent(user.getId(), user.getTenantId()));
    }

    /** Résout un identifiant (username, email ou téléphone) vers un utilisateur — même logique que
     * la connexion, réutilisée pour la demande et la vérification OTP. */
    private Optional<User> resolveIdentifier(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> resolveByPhone(identifier));
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", command.userId()));

        if (!passwordHasher.verify(command.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }

        user.setPasswordHash(passwordHasher.hash(command.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public MeResult me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return new MeResult(user.getId(), user.getTenantId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.getPhoto(), rolesOf(userId), permissionsOf(userId));
    }

    /** Rejects login/refresh when the user's tenant is suspended, archived or terminated (403). */
    /**
     * Résolution d'un utilisateur par numéro de téléphone au login : on essaie plusieurs formats
     * (brut, chiffres seuls, international +224…) pour accepter aussi bien un numéro local qu'international.
     */
    private Optional<User> resolveByPhone(String identifier) {
        for (String candidate : gn.lacaeducation.common.PhoneNumbers.candidates(identifier)) {
            Optional<User> u = userRepository.findByPhone(candidate);
            if (u.isPresent()) return u;
        }
        return Optional.empty();
    }

    private void requireActiveTenant(User user) {
        if (user.getTenantId() != null && !tenantInfoProvider.isTenantActive(user.getTenantId())) {
            throw new DisabledException("Tenant access is currently suspended");
        }
    }

    private List<String> rolesOf(Long userId) {
        return roleRepository.findUserRoles(userId).stream()
                .filter(r -> r.getCode() != null)
                .map(r -> r.getCode().name())
                .toList();
    }

    private List<String> permissionsOf(Long userId) {
        return permissionRepository.findByUserId(userId).stream()
                .map(Permission::getCode)
                .toList();
    }

    /**
     * Effective permissions embedded in the JWT. SUPER_ADMIN is a platform-wide superuser and is
     * granted the entire permission catalogue, so it can access everything regardless of explicit
     * role-permission assignments (and automatically inherits any newly added permission).
     */
    private List<String> permissionsFor(Long userId, List<String> roles) {
        if (roles.contains("SUPER_ADMIN")) {
            return permissionRepository.findAll().stream().map(Permission::getCode).distinct().toList();
        }
        return permissionsOf(userId);
    }

    private String generateRefreshTokenForUser(Long userId) {
        String tokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }
}
