package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.application.port.out.MonthlyReportCardRepository;
import gn.lacaeducation.grading.domain.MonthlyReportCard;
import gn.lacaeducation.grading.domain.MonthlyReportCardLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class MonthlyReportCardPersistenceAdapter implements MonthlyReportCardRepository {
    private final MonthlyReportCardJpaRepository jpa;
    private final MonthlyReportCardLineJpaRepository lineJpa;
    private final GradingMapper mapper;

    @Override public MonthlyReportCard save(MonthlyReportCard c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<MonthlyReportCard> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<MonthlyReportCard> findByClassIdAndYearMonth(Long classId, YearMonth ym) {
        return jpa.findByClassIdAndYearMonth(classId, ym.toString()).stream().map(mapper::toDomain).toList();
    }
    @Override public Optional<MonthlyReportCard> findByStudentIdAndClassIdAndYearMonth(Long studentId, Long classId, YearMonth ym) {
        return jpa.findByStudentIdAndClassIdAndYearMonth(studentId, classId, ym.toString()).map(mapper::toDomain);
    }
    @Override public MonthlyReportCardLine saveLine(MonthlyReportCardLine l) { return mapper.toDomain(lineJpa.save(mapper.toEntity(l))); }
    @Override public List<MonthlyReportCardLine> findLines(Long id) { return lineJpa.findByMonthlyReportCardId(id).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteLinesByMonthlyReportCardId(Long id) { lineJpa.deleteByMonthlyReportCardId(id); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
