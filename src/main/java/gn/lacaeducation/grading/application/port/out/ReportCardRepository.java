package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import java.util.List;
import java.util.Optional;
public interface ReportCardRepository {
    ReportCard save(ReportCard reportCard);
    List<ReportCard> saveAll(List<ReportCard> reportCards);
    Optional<ReportCard> findById(Long id);
    List<ReportCard> findByClassIdAndPeriodId(Long classId, Long periodId);
    Optional<ReportCard> findByStudentIdAndPeriodId(Long studentId, Long periodId);
    List<ReportCard> findByStudentId(Long studentId);
    ReportCardLine saveLine(ReportCardLine line);
    List<ReportCardLine> saveAllLines(List<ReportCardLine> lines);
    List<ReportCardLine> findLines(Long reportCardId);
    void deleteLinesByReportCardId(Long reportCardId);
    void deleteById(Long id);
}
