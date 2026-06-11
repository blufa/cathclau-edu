package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
interface GradeJpaRepository extends JpaRepository<GradeJpaEntity, Long> {
    List<GradeJpaEntity> findByAssessmentId(Long assessmentId);
    List<GradeJpaEntity> findByStudentId(Long studentId);
    @Query("SELECT g FROM GradeJpaEntity g WHERE g.studentId = :studentId " +
           "AND g.assessmentId IN (SELECT a.id FROM AssessmentJpaEntity a WHERE a.periodId = :periodId)")
    List<GradeJpaEntity> findByStudentIdAndPeriodId(@Param("studentId") Long studentId,
                                                     @Param("periodId") Long periodId);
}
