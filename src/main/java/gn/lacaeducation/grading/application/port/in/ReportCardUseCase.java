package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.*;
import java.util.List;
import java.util.Optional;
public interface ReportCardUseCase {
    record GenerateReportCardCommand(Long studentId, Long classId, Long periodId) {}
    ReportCard generate(GenerateReportCardCommand command);
    ReportCard publish(Long id);
    Optional<ReportCard> findById(Long id);
    List<ReportCard> findByClass(Long classId, Long periodId);
    Optional<ReportCard> findByStudent(Long studentId, Long periodId);
    List<ReportCardLine> findLines(Long reportCardId);
    void delete(Long id);
}
