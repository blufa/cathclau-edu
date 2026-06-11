package gn.lacaeducation.grading.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
interface ReportCardLineJpaRepository extends JpaRepository<ReportCardLineJpaEntity, Long> {
    List<ReportCardLineJpaEntity> findByReportCardId(Long reportCardId);
    @Modifying
    @Query("DELETE FROM ReportCardLineJpaEntity l WHERE l.reportCardId = :reportCardId")
    void deleteByReportCardId(@Param("reportCardId") Long reportCardId);
}
