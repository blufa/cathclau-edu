package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
interface AssessmentJpaRepository extends JpaRepository<AssessmentJpaEntity, Long> {
    List<AssessmentJpaEntity> findByClassId(Long classId);
    List<AssessmentJpaEntity> findByPeriodId(Long periodId);
    List<AssessmentJpaEntity> findByClassIdAndPeriodId(Long classId, Long periodId);
    List<AssessmentJpaEntity> findByTenantId(Long tenantId);
    List<AssessmentJpaEntity> findByExamId(Long examId);
    boolean existsByTeacherId(Long teacherId);
    boolean existsByClassIdAndSubjectIdAndPeriodIdAndTypeAndLabelAndDate(
            Long classId, Long subjectId, Long periodId, gn.lacaeducation.grading.domain.AssessmentType type,
            String label, LocalDate date);
    boolean existsByClassIdAndSubjectIdAndPeriodIdAndTypeAndLabelAndDateAndIdNot(
            Long classId, Long subjectId, Long periodId, gn.lacaeducation.grading.domain.AssessmentType type,
            String label, LocalDate date, Long excludingId);
}
