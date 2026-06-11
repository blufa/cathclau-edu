package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ExamJpaRepository extends JpaRepository<ExamJpaEntity, Long> {
    List<ExamJpaEntity> findByClassId(Long classId);
    List<ExamJpaEntity> findByPeriodId(Long periodId);
    List<ExamJpaEntity> findByTenantId(Long tenantId);
}
