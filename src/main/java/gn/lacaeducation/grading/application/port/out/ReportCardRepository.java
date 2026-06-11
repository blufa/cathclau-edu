package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import java.util.List;
import java.util.Optional;
public interface ReportCardRepository {
    ReportCard save(ReportCard reportCard);
    Optional<ReportCard> findById(Long id);
    List<ReportCard> findByClassIdAndPeriodId(Long classId, Long periodId);
    Optional<ReportCard> findByStudentIdAndPeriodId(Long studentId, Long periodId);
    ReportCardLine saveLine(ReportCardLine line);
    List<ReportCardLine> findLines(Long reportCardId);
    void deleteById(Long id);
}
