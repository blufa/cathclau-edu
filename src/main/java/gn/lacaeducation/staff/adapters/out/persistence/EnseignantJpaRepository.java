package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

interface EnseignantJpaRepository extends JpaRepository<EnseignantJpaEntity, Long> {
    List<EnseignantJpaEntity> findByTenantId(Long tenantId);
    Optional<EnseignantJpaEntity> findByEmail(String email);
    Optional<EnseignantJpaEntity> findByUserId(Long userId);
    Optional<EnseignantJpaEntity> findByIdempotencyKey(String idempotencyKey);
    boolean existsByTenantIdAndStaffNumber(Long tenantId, String staffNumber);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndActiveTrue(Long tenantId);
    @Query("SELECT t FROM EnseignantJpaEntity t WHERE t.staffNumber IS NULL OR t.staffNumber = ''")
    List<EnseignantJpaEntity> findMissingStaffNumber();
}
