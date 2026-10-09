package gn.lacaeducation.iam.application.port.out;

import gn.lacaeducation.iam.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    /** Connexion par numéro de téléphone (premier utilisateur portant ce numéro). */
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    /** Unicité du username scopée par tenant (tenantId {@code null} = comptes plateforme/SUPER_ADMIN). */
    boolean existsByTenantIdAndUsername(Long tenantId, String username);
    /** Unicité du téléphone scopée par tenant — testée sur toutes les variantes de format
     * ({@link gn.lacaeducation.common.PhoneNumbers#candidates}), voir {@code UserService}. */
    boolean existsByTenantIdAndPhone(Long tenantId, String phone);
    /** Variante « modification » : exclut l'utilisateur dont on modifie le numéro. */
    boolean existsByTenantIdAndPhoneAndIdNot(Long tenantId, String phone, Long id);
    List<User> findByTenantId(Long tenantId);
    /** Tous les utilisateurs, toutes écoles confondues — réservé aux vues plateforme (SUPER_ADMIN). */
    List<User> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
    void assignRole(Long userId, Long roleId);
    /** Enregistre l'activité de l'utilisateur (throttlé en amont). Best-effort : ne doit jamais faire échouer l'appelant. */
    void touchActivity(Long userId);
}
