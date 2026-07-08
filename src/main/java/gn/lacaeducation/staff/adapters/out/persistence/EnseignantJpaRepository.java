package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface EnseignantJpaRepository extends JpaRepository<EnseignantJpaEntity, Long> {
    List<EnseignantJpaEntity> findByTenantId(Long tenantId);
    Optional<EnseignantJpaEntity> findByEmail(String email);
    Optional<EnseignantJpaEntity> findByUserId(Long userId);
    boolean existsByTenantIdAndStaffNumber(Long tenantId, String staffNumber);
}
