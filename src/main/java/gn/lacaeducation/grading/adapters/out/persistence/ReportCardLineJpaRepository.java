package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ReportCardLineJpaRepository extends JpaRepository<ReportCardLineJpaEntity, Long> {
    List<ReportCardLineJpaEntity> findByReportCardId(Long reportCardId);
}
