package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface MonthlyReportCardLineJpaRepository extends JpaRepository<MonthlyReportCardLineJpaEntity, Long> {
    List<MonthlyReportCardLineJpaEntity> findByMonthlyReportCardId(Long monthlyReportCardId);
    void deleteByMonthlyReportCardId(Long monthlyReportCardId);
}
