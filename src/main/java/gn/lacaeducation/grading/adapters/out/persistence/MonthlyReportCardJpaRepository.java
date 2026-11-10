package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface MonthlyReportCardJpaRepository extends JpaRepository<MonthlyReportCardJpaEntity, Long> {
    List<MonthlyReportCardJpaEntity> findByClassIdAndYearMonth(Long classId, String yearMonth);
    Optional<MonthlyReportCardJpaEntity> findByStudentIdAndClassIdAndYearMonth(Long studentId, Long classId, String yearMonth);
}
