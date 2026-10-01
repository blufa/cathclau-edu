package gn.lacaeducation.students.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

interface ParentJpaRepository extends JpaRepository<ParentJpaEntity, Long> {
    List<ParentJpaEntity> findByTenantId(Long tenantId);
    Optional<ParentJpaEntity> findByUserId(Long userId);
    List<ParentJpaEntity> findAllByUserId(Long userId);
    List<ParentJpaEntity> findAllByLinkedStaffTypeAndLinkedStaffId(
            gn.lacaeducation.students.domain.StaffLinkType linkedStaffType, Long linkedStaffId);
    Optional<ParentJpaEntity> findByIdempotencyKey(String idempotencyKey);

    /** Suggestions de prénom/nom/adresse déjà saisis pour d'autres parents du même tenant (§retour
     * comptable) — {@code q} déjà en minuscules. */
    @Query("SELECT DISTINCT p.firstName FROM ParentJpaEntity p WHERE p.tenantId = :tenantId "
            + "AND LOWER(p.firstName) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY p.firstName")
    List<String> suggestFirstNames(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT p.lastName FROM ParentJpaEntity p WHERE p.tenantId = :tenantId "
            + "AND LOWER(p.lastName) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY p.lastName")
    List<String> suggestLastNames(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

    @Query("SELECT DISTINCT p.address FROM ParentJpaEntity p WHERE p.tenantId = :tenantId AND p.address IS NOT NULL "
            + "AND LOWER(p.address) LIKE CONCAT('%', CAST(:q AS string), '%') ORDER BY p.address")
    List<String> suggestAddresses(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);
}
