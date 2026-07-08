package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reacts to platform tenant lifecycle events: provisions the initial SCHOOL_ADMIN account on tenant
 * creation and invalidates active sessions when a tenant/subscription is suspended. Default roles and
 * permissions are seeded globally by Flyway (V3/V16), so provisioning only creates the admin user and
 * binds it to the SCHOOL_ADMIN role.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;

    /**
     * Creates the tenant's ADMIN_ECOLE (SCHOOL_ADMIN) user. Idempotent on (tenant, email).
     * {@code temporaryPassword} forces a password change at first login (mot de passe généré, envoyé
     * par email). A blank {@code rawPassword} falls back to a random hash (defensive).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionTenantAdmin(Long tenantId, String schoolName, String adminEmail, String director,
                                     String rawPassword, boolean temporaryPassword) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Cannot provision admin for tenant {}: no admin email provided", tenantId);
            return;
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, adminEmail)) {
            log.info("Admin user already exists for tenant {} ({})", tenantId, adminEmail);
            return;
        }

        String[] name = splitName(director, schoolName);
        boolean hasPassword = rawPassword != null && !rawPassword.isBlank();
        User admin = User.builder()
                .tenantId(tenantId)
                .firstName(name[0])
                .lastName(name[1])
                .username(UsernameGenerator.generate(name[0], name[1], userRepository::existsByUsername))
                .email(adminEmail)
                .passwordHash(passwordHasher.hash(hasPassword ? rawPassword : UUID.randomUUID().toString()))
                .mustChangePassword(temporaryPassword || !hasPassword)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(admin);

        Role schoolAdmin = roleRepository.findByCode(RoleCode.SCHOOL_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SCHOOL_ADMIN role not seeded"));
        userRepository.assignRole(saved.getId(), schoolAdmin.getId());
        log.info("Provisioned SCHOOL_ADMIN user {} for tenant {}", saved.getId(), tenantId);
    }

    /**
     * Creates the tenant's ADMIN_ECOLE as <b>unverified</b> (inactive) with the password chosen at
     * self-service signup. The account is activated once the director verifies their email
     * ({@link #markAdminVerified}). Idempotent on (tenant, email).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void provisionUnverifiedAdmin(Long tenantId, String schoolName, String adminEmail,
                                         String director, String rawPassword,
                                         String adminFirstName, String adminLastName, String adminPhone) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Cannot provision admin for tenant {}: no admin email provided", tenantId);
            return;
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, adminEmail)) {
            log.info("Admin user already exists for tenant {} ({})", tenantId, adminEmail);
            return;
        }
        // Nom de l'administrateur fourni au signup ; repli sur le directeur/école si absent.
        String firstName = (adminFirstName != null && !adminFirstName.isBlank()) ? adminFirstName : null;
        String lastName = (adminLastName != null && !adminLastName.isBlank()) ? adminLastName : null;
        if (firstName == null || lastName == null) {
            String[] name = splitName(director, schoolName);
            if (firstName == null) firstName = name[0];
            if (lastName == null) lastName = name[1];
        }
        String hash = passwordHasher.hash(
                rawPassword != null && !rawPassword.isBlank() ? rawPassword : UUID.randomUUID().toString());
        User admin = User.builder()
                .tenantId(tenantId)
                .firstName(firstName)
                .lastName(lastName)
                .username(UsernameGenerator.generate(firstName, lastName, userRepository::existsByUsername))
                .email(adminEmail)
                .phone(adminPhone)
                .passwordHash(hash)
                .active(false) // unverified until email is confirmed
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(admin);

        Role schoolAdmin = roleRepository.findByCode(RoleCode.SCHOOL_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SCHOOL_ADMIN role not seeded"));
        userRepository.assignRole(saved.getId(), schoolAdmin.getId());
        log.info("Provisioned unverified SCHOOL_ADMIN user {} for tenant {}", saved.getId(), tenantId);
    }

    /** Activates the tenant's admin account once the email is verified. Idempotent. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAdminVerified(Long tenantId, String adminEmail) {
        if (tenantId == null || adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        userRepository.findByEmailAndTenantId(adminEmail, tenantId).ifPresent(u -> {
            if (!Boolean.TRUE.equals(u.getActive())) {
                u.setActive(true);
                userRepository.save(u);
                log.info("Marked admin {} of tenant {} as verified/active", u.getId(), tenantId);
            }
        });
    }

    /** Revokes every refresh token of a tenant's users, forcing re-authentication. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeTenantSessions(Long tenantId) {
        if (tenantId == null) return;
        userRepository.findByTenantId(tenantId)
                .forEach(u -> refreshTokenRepository.revokeAllByUserId(u.getId()));
        log.info("Revoked active sessions for tenant {}", tenantId);
    }

    private String[] splitName(String director, String schoolName) {
        if (director != null && !director.isBlank()) {
            String[] parts = director.trim().split("\\s+", 2);
            return parts.length == 2 ? new String[]{parts[0], parts[1]} : new String[]{"Admin", parts[0]};
        }
        return new String[]{"Admin", schoolName != null && !schoolName.isBlank() ? schoolName : "School"};
    }
}
