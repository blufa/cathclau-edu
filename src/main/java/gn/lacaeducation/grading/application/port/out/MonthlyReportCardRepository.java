package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.MonthlyReportCard;
import gn.lacaeducation.grading.domain.MonthlyReportCardLine;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
public interface MonthlyReportCardRepository {
    MonthlyReportCard save(MonthlyReportCard card);
    Optional<MonthlyReportCard> findById(Long id);
    List<MonthlyReportCard> findByClassIdAndYearMonth(Long classId, YearMonth yearMonth);
    Optional<MonthlyReportCard> findByStudentIdAndClassIdAndYearMonth(Long studentId, Long classId, YearMonth yearMonth);
    MonthlyReportCardLine saveLine(MonthlyReportCardLine line);
    List<MonthlyReportCardLine> findLines(Long monthlyReportCardId);
    void deleteLinesByMonthlyReportCardId(Long monthlyReportCardId);
    void deleteById(Long id);
}
