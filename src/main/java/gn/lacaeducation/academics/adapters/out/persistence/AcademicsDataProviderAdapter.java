package gn.lacaeducation.academics.adapters.out.persistence;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.AcademicYearPublicInfo;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.academics.PeriodPublicInfo;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Collection;
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
    private final EmploiDuTempsJpaRepository emploiDuTempsRepo;
    private final AnneeScolaireJpaRepository anneeRepo;
    private final NiveauJpaRepository niveauRepo;
    @Override
    public Optional<ClassPublicInfo> findClass(Long classId) {
        return classeRepo.findById(classId).map(this::toClassPublicInfo);
    }

    /** Résout le niveau de la classe (libellé + cycle scolaire) — cycle utilisé notamment pour
     * déclencher le badge de retrait automatique à la sortie d'un enfant de maternelle/primaire
     * (§attendance). */
    private ClassPublicInfo toClassPublicInfo(ClasseJpaEntity c) {
        NiveauJpaEntity niveau = c.getLevelId() != null ? niveauRepo.findById(c.getLevelId()).orElse(null) : null;
        return new ClassPublicInfo(c.getId(), c.getLabel(), niveau != null ? niveau.getLabel() : null,
                c.getClassType() != null ? c.getClassType().name() : null,
                c.getClassRepresentativeStudentId(), c.getHeadTeacherId(),
                niveau != null && niveau.getCycle() != null ? niveau.getCycle().name() : null);
    }
    @Override
    public Map<Long, String> subjectLabels(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return Map.of();
        return matiereRepo.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(MatiereJpaEntity::getId, MatiereJpaEntity::getLabel));
    }
    @Override
    public Optional<PeriodPublicInfo> findPeriod(Long periodId) {
        return periodeRepo.findById(periodId).map(p ->
                new PeriodPublicInfo(p.getId(), p.getLabel(), p.getType() != null ? p.getType().name() : null,
                        p.getStartDate(), p.getEndDate()));
    }
    @Override
    public List<PeriodPublicInfo> findPeriodsByAcademicYear(Long academicYearId) {
        return periodeRepo.findByAcademicYearIdOrderBySortOrder(academicYearId).stream()
                .map(p -> new PeriodPublicInfo(p.getId(), p.getLabel(), p.getType() != null ? p.getType().name() : null,
                        p.getStartDate(), p.getEndDate()))
                .toList();
    }

    @Override
    public boolean isPeriodOpenForGrades(Long periodId) {
        return periodeRepo.findById(periodId)
                .filter(p -> !Boolean.TRUE.equals(p.getClosed()))
                .flatMap(p -> anneeRepo.findById(p.getAcademicYearId()))
                .map(y -> y.getStatus() == gn.lacaeducation.academics.domain.valueobject.StatutAnneeScolaire.EN_COURS)
                .orElse(false);
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
                        a.getSchoolClassId(),
                        a.getSubjectId(),
                        coeffBySubject.getOrDefault(a.getSubjectId(), BigDecimal.ONE),
                        a.getTeacherId(),
                        a.getCode()))
                .toList();
    }

    @Override
    public List<ClassPublicInfo> findClassesByHeadTeacher(Long teacherId) {
        return classeRepo.findByHeadTeacherId(teacherId).stream()
                .map(this::toClassPublicInfo)
                .toList();
    }

    @Override
    public List<ClassSubjectPublicInfo> findClassSubjectsByTeacher(Long teacherId) {
        List<ClasseMatiereJpaEntity> assignments = classeMatiereRepo.findByTeacherId(teacherId);
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
                        a.getSchoolClassId(),
                        a.getSubjectId(),
                        coeffBySubject.getOrDefault(a.getSubjectId(), BigDecimal.ONE),
                        a.getTeacherId(),
                        a.getCode()))
                .toList();
    }

    @Override
    public List<ScheduleSlotPublicInfo> findScheduleByTeacher(Long teacherId) {
        return emploiDuTempsRepo.findByTeacherId(teacherId).stream().map(this::toSlot).toList();
    }

    @Override
    public List<ScheduleSlotPublicInfo> findScheduleByClass(Long classId) {
        return emploiDuTempsRepo.findBySchoolClassId(classId).stream().map(this::toSlot).toList();
    }

    @Override
    public List<ClassPublicInfo> findClassesByTenant(Long tenantId) {
        return classeRepo.findByTenantId(tenantId).stream()
                .map(this::toClassPublicInfo)
                .toList();
    }

    @Override
    public Optional<String> currentAcademicYearLabel(Long tenantId) {
        return anneeRepo.findByTenantIdAndCurrentTrue(tenantId).map(AnneeScolaireJpaEntity::getLabel);
    }

    @Override
    public Optional<AcademicYearPublicInfo> currentAcademicYear(Long tenantId) {
        return anneeRepo.findByTenantIdAndCurrentTrue(tenantId)
                .map(y -> new AcademicYearPublicInfo(y.getId(), y.getLabel(), y.getStartDate(), y.getEndDate()));
    }

    @Override
    public Optional<AcademicYearPublicInfo> findAcademicYear(Long academicYearId) {
        return anneeRepo.findById(academicYearId)
                .map(y -> new AcademicYearPublicInfo(y.getId(), y.getLabel(), y.getStartDate(), y.getEndDate()));
    }

    private ScheduleSlotPublicInfo toSlot(EmploiDuTempsJpaEntity e) {
        return new ScheduleSlotPublicInfo(
                e.getId(), e.getSchoolClassId(), e.getSubjectId(), e.getTeacherId(), e.getRoomId(),
                e.getDay() != null ? e.getDay().name() : null,
                e.getStartTime() != null ? e.getStartTime().toString() : null,
                e.getEndTime() != null ? e.getEndTime().toString() : null);
    }
}
