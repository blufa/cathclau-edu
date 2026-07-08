package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AssessmentTypeConfigJpaRepository extends JpaRepository<AssessmentTypeConfigJpaEntity, Long> {
    List<AssessmentTypeConfigJpaEntity> findByTenantId(Long tenantId);
    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
