package gn.lacaeducation.iam.application.port.in;

import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserUseCase {
    User create(CreateUserCommand command);
    Optional<User> findById(Long id);
    List<User> findByTenantId(Long tenantId);
    /** Tous les utilisateurs, toutes écoles confondues — réservé aux vues plateforme (SUPER_ADMIN). */
    List<User> findAll();
    User update(Long id, UpdateUserCommand command);
    void delete(Long id);
    void assignRole(Long userId, Long roleId);
    List<Role> findUserRoles(Long userId);

    /**
     * (Ré)initialise l'accès d'un compte : définit le mot de passe (fourni, sinon temporaire généré),
     * impose son changement à la prochaine connexion et révoque les sessions. Retourne l'identifiant
     * et le mot de passe en clair à communiquer une seule fois.
     */
    TemporaryAccess resetAccess(Long userId, String rawPassword);

    record CreateUserCommand(
        Long tenantId,
        String firstName,
        String lastName,
        String email,
        /** Optionnel : si absent, un mot de passe temporaire est généré et devra être changé. */
        String password,
        String phone
    ) {}

    /** Identifiants restitués une seule fois après création/réinitialisation d'un accès. */
    record TemporaryAccess(Long userId, String username, String temporaryPassword, boolean mustChangePassword) {}

    record UpdateUserCommand(
        String firstName,
        String lastName,
        String phone,
        Boolean active
    ) {}
}
