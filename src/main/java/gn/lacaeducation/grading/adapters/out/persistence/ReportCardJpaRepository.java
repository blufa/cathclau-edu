package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface ReportCardJpaRepository extends JpaRepository<ReportCardJpaEntity, Long> {
    List<ReportCardJpaEntity> findByClassIdAndPeriodId(Long classId, Long periodId);
    Optional<ReportCardJpaEntity> findByStudentIdAndPeriodId(Long studentId, Long periodId);
    List<ReportCardJpaEntity> findByStudentId(Long studentId);
}
