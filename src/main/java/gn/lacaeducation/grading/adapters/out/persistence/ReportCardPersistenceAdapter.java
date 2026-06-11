package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ReportCardPersistenceAdapter implements ReportCardRepository {
    private final ReportCardJpaRepository jpa;
    private final ReportCardLineJpaRepository lineJpa;
    private final GradingMapper mapper;
    @Override public ReportCard save(ReportCard rc) { return mapper.toDomain(jpa.save(mapper.toEntity(rc))); }
    @Override public Optional<ReportCard> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ReportCard> findByClassIdAndPeriodId(Long c, Long p) { return jpa.findByClassIdAndPeriodId(c, p).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<ReportCard> findByStudentIdAndPeriodId(Long s, Long p) { return jpa.findByStudentIdAndPeriodId(s, p).map(mapper::toDomain); }
    @Override public ReportCardLine saveLine(ReportCardLine l) { return mapper.toDomain(lineJpa.save(mapper.toEntity(l))); }
    @Override public List<ReportCardLine> findLines(Long id) { return lineJpa.findByReportCardId(id).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
