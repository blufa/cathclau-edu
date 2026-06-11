package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface AssessmentJpaRepository extends JpaRepository<AssessmentJpaEntity, Long> {
    List<AssessmentJpaEntity> findByClassId(Long classId);
    List<AssessmentJpaEntity> findByPeriodId(Long periodId);
    List<AssessmentJpaEntity> findByTenantId(Long tenantId);
}
