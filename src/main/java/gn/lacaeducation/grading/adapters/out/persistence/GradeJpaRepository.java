package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface GradeJpaRepository extends JpaRepository<GradeJpaEntity, Long> {
    List<GradeJpaEntity> findByAssessmentId(Long assessmentId);
    List<GradeJpaEntity> findByStudentId(Long studentId);
}
