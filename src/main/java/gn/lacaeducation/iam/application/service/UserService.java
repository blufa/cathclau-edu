package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.PersonName;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.event.UserAccountCreatedEvent;
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
        boolean commandHasPhone = command.phone() != null && !command.phone().isBlank();
        boolean commandHasEmail = command.email() != null && !command.email().isBlank();
        if (!commandHasPhone && !commandHasEmail) {
            throw new gn.lacaeducation.common.BusinessRuleException(
                    "Un numéro de téléphone ou un email est requis pour créer un compte.");
        }
        if (commandHasEmail && userRepository.existsByTenantIdAndEmail(command.tenantId(), command.email())) {
            throw new ConflictException("A user with this email already exists in this tenant");
        }
        if (commandHasPhone && phoneTakenInTenant(command.tenantId(), command.phone(), null)) {
            throw new ConflictException("Un utilisateur avec ce numéro de téléphone existe déjà dans cet établissement.");
        }
        // Identifiant de connexion généré (prenom.nom, unique PAR TENANT) — la même personne peut
        // avoir un compte dans plusieurs écoles avec le même prénom/nom sans collision.
        String username = UsernameGenerator.generate(command.firstName(), command.lastName(),
                candidate -> userRepository.existsByTenantIdAndUsername(command.tenantId(), candidate));
        // Mot de passe : celui choisi par l'admin, sinon un mot de passe temporaire à changer.
        boolean generated = command.password() == null || command.password().isBlank();
        String rawPassword = generated ? TemporaryPassword.generate() : command.password();
        User user = User.builder()
                .tenantId(command.tenantId())
                .firstName(PersonName.capitalizeFirstName(command.firstName()))
                .lastName(PersonName.upperCaseLastName(command.lastName()))
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
        // Envoi des identifiants (email de préférence, sinon SMS — §2.2) : rawPassword n'existe qu'en
        // mémoire à cet instant, jamais persisté en clair — d'où la publication immédiate, avant qu'il
        // ne soit plus disponible.
        boolean hasPhone = command.phone() != null && !command.phone().isBlank();
        boolean hasEmail = command.email() != null && !command.email().isBlank();
        if (hasPhone || hasEmail) {
            eventPublisher.publishEvent(new UserAccountCreatedEvent(saved.getTenantId(), saved.getId(),
                    command.phone(), command.email(), saved.getFirstName(), saved.getLastName(), username, rawPassword));
        }
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
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(Long id, UpdateUserCommand command) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        // Partial update: only overwrite fields explicitly provided, so PATCH activate/deactivate
        // (which carries only the active flag) does not wipe the user's name/phone.
        if (command.firstName() != null) user.setFirstName(PersonName.capitalizeFirstName(command.firstName()));
        if (command.lastName() != null) user.setLastName(PersonName.upperCaseLastName(command.lastName()));
        if (command.phone() != null && !command.phone().equals(user.getPhone())) {
            // Sans ce contrôle, réassigner un numéro déjà utilisé par un autre compte du même
            // établissement casse silencieusement la connexion des deux (résolution ambiguë au
            // login, voir AuthService#resolveByPhone) : on bloque explicitement au lieu de laisser
            // l'admin découvrir le problème quand l'utilisateur ne peut plus se connecter.
            if (!command.phone().isBlank() && phoneTakenInTenant(user.getTenantId(), command.phone(), id)) {
                throw new ConflictException("Un utilisateur avec ce numéro de téléphone existe déjà dans cet établissement.");
            }
            user.setPhone(command.phone());
        }
        if (command.photo() != null) user.setPhoto(command.photo());
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

    /** Teste toutes les variantes de format ({@code "622...", "+224622...", "00224622..."}) du même
     * numéro — sans ça, un numéro stocké dans un format et ressaisi dans un autre ne serait pas
     * détecté comme doublon (voir {@code AuthService#resolveByPhone}, qui doit reconnaître les mêmes
     * numéros comme identiques). {@code excludeUserId} null en création, id de l'utilisateur en modification. */
    private boolean phoneTakenInTenant(Long tenantId, String phone, Long excludeUserId) {
        for (String candidate : gn.lacaeducation.common.PhoneNumbers.candidates(phone)) {
            boolean taken = excludeUserId == null
                    ? userRepository.existsByTenantIdAndPhone(tenantId, candidate)
                    : userRepository.existsByTenantIdAndPhoneAndIdNot(tenantId, candidate, excludeUserId);
            if (taken) return true;
        }
        return false;
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
