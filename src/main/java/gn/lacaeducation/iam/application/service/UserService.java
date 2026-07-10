package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public User create(CreateUserCommand command) {
        if (command.email() != null && userRepository.existsByTenantIdAndEmail(command.tenantId(), command.email())) {
            throw new ConflictException("A user with this email already exists in this tenant");
        }
        // Identifiant de connexion généré (prenom.nom, unique) pour les comptes créés par l'école.
        String username = UsernameGenerator.generate(
                command.firstName(), command.lastName(), userRepository::existsByUsername);
        // Mot de passe : celui choisi par l'admin, sinon un mot de passe temporaire à changer.
        boolean generated = command.password() == null || command.password().isBlank();
        String rawPassword = generated ? TemporaryPassword.generate() : command.password();
        User user = User.builder()
                .tenantId(command.tenantId())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .username(username)
                .email(command.email())
                .passwordHash(passwordHasher.hash(rawPassword))
                .mustChangePassword(generated)
                .phone(command.phone())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        // On restitue le mot de passe uniquement quand il a été généré (jamais persisté en clair).
        if (generated) saved.setPlainTemporaryPassword(rawPassword);
        return saved;
    }

    @Override
    public TemporaryAccess resetAccess(Long userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        boolean generated = rawPassword == null || rawPassword.isBlank();
        String password = generated ? TemporaryPassword.generate() : rawPassword;
        user.setPasswordHash(passwordHasher.hash(password));
        // Mot de passe généré (temporaire) → à changer à la 1re connexion. Mot de passe choisi
        // explicitement par l'admin → définitif, aucun changement forcé.
        user.setMustChangePassword(generated);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);
        return new TemporaryAccess(userId, user.getUsername(), password, generated);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByTenantId(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Override
    public User update(Long id, UpdateUserCommand command) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        // Partial update: only overwrite fields explicitly provided, so PATCH activate/deactivate
        // (which carries only the active flag) does not wipe the user's name/phone.
        if (command.firstName() != null) user.setFirstName(command.firstName());
        if (command.lastName() != null) user.setLastName(command.lastName());
        if (command.phone() != null) user.setPhone(command.phone());
        if (command.active() != null) {
            user.setActive(command.active());
            if (Boolean.FALSE.equals(command.active())) {
                refreshTokenRepository.revokeAllByUserId(id);
            }
        }
        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {
        // Soft delete: mark the row deleted (never physically remove) and revoke active sessions.
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(id);
    }

    @Override
    public void assignRole(Long userId, Long roleId) {
        userRepository.assignRole(userId, roleId);
        eventPublisher.publishEvent(new RoleAssignedEvent(userId, roleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findUserRoles(Long userId) {
        return roleRepository.findUserRoles(userId);
    }
}
