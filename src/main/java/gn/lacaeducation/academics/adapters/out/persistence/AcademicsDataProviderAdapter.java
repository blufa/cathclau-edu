package gn.lacaeducation.academics.adapters.out.persistence;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.PeriodPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Component @RequiredArgsConstructor
public class AcademicsDataProviderAdapter implements AcademicsDataProvider {
    private final ClasseJpaRepository classeRepo;
    private final PeriodeJpaRepository periodeRepo;
    private final ClasseMatiereJpaRepository classeMatiereRepo;
    private final MatiereJpaRepository matiereRepo;
    @Override
    public Optional<ClassPublicInfo> findClass(Long classId) {
        return classeRepo.findById(classId).map(c -> new ClassPublicInfo(c.getId(), c.getLabel(), null));
    }
    @Override
    public Optional<PeriodPublicInfo> findPeriod(Long periodId) {
        return periodeRepo.findById(periodId).map(p ->
                new PeriodPublicInfo(p.getId(), p.getLabel(), p.getType() != null ? p.getType().name() : null));
    }
    @Override
    public List<ClassSubjectPublicInfo> findClassSubjects(Long classId) {
        List<ClasseMatiereJpaEntity> assignments = classeMatiereRepo.findBySchoolClassId(classId);
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> subjectIds = assignments.stream()
                .map(ClasseMatiereJpaEntity::getSubjectId)
                .collect(Collectors.toSet());
        Map<Long, BigDecimal> coeffBySubject = matiereRepo.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        MatiereJpaEntity::getId,
                        s -> s.getCoefficient() != null ? s.getCoefficient() : BigDecimal.ONE));
        return assignments.stream()
                .map(a -> new ClassSubjectPublicInfo(
                        a.getSubjectId(),
                        coeffBySubject.getOrDefault(a.getSubjectId(), BigDecimal.ONE),
                        a.getTeacherId()))
                .toList();
    }
}
