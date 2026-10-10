package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByUsername(String username);
    /** {@code OrderByIdAsc} : sans tri explicite, "premier" n'est pas garanti stable d'un appel à
     * l'autre (plan de requête, VACUUM...) — si un doublon de téléphone existe encore en base (numéro
     * créé avant la contrainte d'unicité ajoutée dans UserService), résoudre tantôt vers l'un tantôt
     * vers l'autre ferait échouer la connexion après une réinitialisation de mot de passe par OTP
     * (code envoyé/validé sur un compte, mot de passe modifié sur un autre). Id croissant = le compte
     * le plus ancien, choix déterministe et stable. */
    Optional<UserJpaEntity> findFirstByPhoneOrderByIdAsc(String phone);
    Optional<UserJpaEntity> findByEmailAndTenantId(String email, Long tenantId);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    /** Détection de doublon avant enregistrement (création ou modification du numéro) — même tenant. */
    boolean existsByTenantIdAndPhone(Long tenantId, String phone);
    /** Variante utilisée en modification : exclut l'utilisateur lui-même (son propre numéro inchangé
     * ne doit pas se signaler comme un doublon). */
    boolean existsByTenantIdAndPhoneAndIdNot(Long tenantId, String phone, Long id);
    /** {@code tenantId} peut être {@code null} (comptes SUPER_ADMIN, hors tenant) : comparaison explicite pour gérer NULL correctement. */
    @Query("SELECT COUNT(u) > 0 FROM UserJpaEntity u WHERE u.username = :username "
            + "AND ((:tenantId IS NULL AND u.tenantId IS NULL) OR u.tenantId = :tenantId)")
    boolean existsByTenantIdAndUsername(@Param("tenantId") Long tenantId, @Param("username") String username);
    List<UserJpaEntity> findByTenantId(Long tenantId);

    /** Horodatage d'activité (dernière requête authentifiée) — mis à jour à chaque requête, throttlé
     * en amont par {@code UserActivityFilter} pour ne pas écrire à chaque appel. */
    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.lastActivityAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void touchActivity(@Param("id") Long id);

    /** Comptes actifs (email renseigné) du tenant portant l'un des rôles donnés. */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.tenantId = :tenantId AND u.active = true "
            + "AND u.email IS NOT NULL AND u.id IN "
            + "(SELECT ur.userId FROM UserRoleJpaEntity ur WHERE ur.roleId IN "
            + "(SELECT r.id FROM RoleJpaEntity r WHERE r.code IN :codes))")
    List<UserJpaEntity> findActiveByTenantIdAndRoleCodes(@Param("tenantId") Long tenantId,
                                                         @Param("codes") Collection<RoleCode> codes);
}
