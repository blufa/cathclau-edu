package gn.lacaeducation.audit.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    Page<AuditLogJpaEntity> findByTenantIdIsNull(Pageable pageable);

    // Filtres optionnels (:param IS NULL OU champ = :param) — le filtre Hibernate tenantFilter
    // s'applique comme pour findAll(), aucun besoin de contrainte explicite sur tenant_id ici.
    // CAST explicite sur :from/:to : sans lui, PostgreSQL ne peut pas déterminer le type du
    // paramètre dans "? IS NULL" quand il n'apparaît dans AUCUNE AUTRE position du même type dans la
    // requête préparée ("ERROR: could not determine data type of parameter $6") — bug constaté en
    // prod, reproduit en base réelle : tout appel avec from/to nuls (donc TOUS les filtres par défaut,
    // action ou utilisateur compris) renvoyait un 500, jamais seulement ceux filtrant par date.
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE "
            + "(:action IS NULL OR a.action = :action) AND "
            + "(:utilisateurId IS NULL OR a.utilisateurId = :utilisateurId) AND "
            + "(CAST(:from AS java.time.LocalDateTime) IS NULL OR a.createdAt >= :from) AND "
            + "(CAST(:to AS java.time.LocalDateTime) IS NULL OR a.createdAt <= :to)")
    Page<AuditLogJpaEntity> search(@Param("action") String action, @Param("utilisateurId") Long utilisateurId,
                                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                    Pageable pageable);
}
