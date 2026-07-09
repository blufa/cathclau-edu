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
    @Override public List<ReportCard> saveAll(List<ReportCard> rcs) {
        List<ReportCardJpaEntity> entities = rcs.stream().map(mapper::toEntity).toList();
        return jpa.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
    @Override public Optional<ReportCard> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ReportCard> findByClassIdAndPeriodId(Long c, Long p) { return jpa.findByClassIdAndPeriodId(c, p).stream().map(mapper::toDomain).toList(); }
    @Override public List<ReportCard> findByPeriodId(Long p) { return jpa.findByPeriodId(p).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<ReportCard> findByStudentIdAndPeriodId(Long s, Long p) { return jpa.findByStudentIdAndPeriodId(s, p).map(mapper::toDomain); }
    @Override public List<ReportCard> findByStudentId(Long studentId) { return jpa.findByStudentId(studentId).stream().map(mapper::toDomain).toList(); }
    @Override public ReportCardLine saveLine(ReportCardLine l) { return mapper.toDomain(lineJpa.save(mapper.toEntity(l))); }
    @Override public List<ReportCardLine> saveAllLines(List<ReportCardLine> lines) {
        List<ReportCardLineJpaEntity> entities = lines.stream().map(mapper::toEntity).toList();
        return lineJpa.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
    @Override public List<ReportCardLine> findLines(Long id) { return lineJpa.findByReportCardId(id).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteLinesByReportCardId(Long reportCardId) { lineJpa.deleteByReportCardId(reportCardId); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
