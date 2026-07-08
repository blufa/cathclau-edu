package gn.lacaeducation.billing.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ExemptionJpaRepository extends JpaRepository<ExemptionJpaEntity, Long> {
    List<ExemptionJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<ExemptionJpaEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
