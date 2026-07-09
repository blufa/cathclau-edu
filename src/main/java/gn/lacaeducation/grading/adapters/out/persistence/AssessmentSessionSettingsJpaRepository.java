package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface AssessmentSessionSettingsJpaRepository extends JpaRepository<AssessmentSessionSettingsJpaEntity, Long> {
    Optional<AssessmentSessionSettingsJpaEntity> findByTenantId(Long tenantId);
}
