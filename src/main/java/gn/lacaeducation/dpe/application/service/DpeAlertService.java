package gn.lacaeducation.dpe.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.dpe.application.port.in.DpeAlertUseCase;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase.SchoolSummary;
import gn.lacaeducation.dpe.application.port.out.DpeAlertRepository;
import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertPriority;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Règles d'alerte de la première tranche (dérivées des effectifs déjà exposés par le tableau de
 * bord — voir DPE_ARCHITECTURE_ANALYSIS.md « Reste à faire » pour les règles fondées sur la
 * présence/les résultats, qui nécessitent une agrégation non encore construite).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DpeAlertService implements DpeAlertUseCase {

    /** Ratio élèves/enseignant au-delà duquel une alerte indicative est levée — configurable dans le
     * code pour l'instant ; à exposer comme paramètre tenant/plateforme si le besoin se confirme. */
    private static final BigDecimal HIGH_RATIO_THRESHOLD = BigDecimal.valueOf(40);

    private final DpeAlertRepository repository;
    private final DpeDashboardUseCase dashboardUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<DpeAlert> findForPrefecture(Long prefectureId, DpeAlertStatus statutFilter) {
        return statutFilter != null
                ? repository.findByPrefectureIdAndStatut(prefectureId, statutFilter)
                : repository.findByPrefectureId(prefectureId);
    }

    @Override
    public DpeAlert updateStatus(Long prefectureId, Long alertId, DpeAlertStatus newStatus, Long userId) {
        DpeAlert alert = repository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("DpeAlert", alertId));
        if (!alert.getPrefectureId().equals(prefectureId)) {
            throw new AccessDeniedException("Cette alerte n'appartient pas à votre périmètre.");
        }
        alert.getStatut().requireTransitionAllowed(newStatus);
        alert.setStatut(newStatus);
        alert.setUpdatedAt(LocalDateTime.now());
        if (newStatus == DpeAlertStatus.RESOLUE) {
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedByUserId(userId);
        } else {
            alert.setResolvedAt(null);
            alert.setResolvedByUserId(null);
        }
        return repository.save(alert);
    }

    @Override
    public void evaluateForPrefecture(Long prefectureId, Set<Long> tenantIds) {
        List<SchoolSummary> schools = dashboardUseCase.schools(tenantIds);
        for (SchoolSummary s : schools) {
            evaluateSchool(prefectureId, s);
        }
        log.info("Évaluation des alertes DPE : préfecture={}, {} école(s) évaluée(s)", prefectureId, schools.size());
    }

    private void evaluateSchool(Long prefectureId, SchoolSummary s) {
        Long tenantId = s.tenantId();

        rule(prefectureId, tenantId, DpeAlertType.SCHOOL_NO_STUDENTS, s.studentCount() == 0,
                DpeAlertPriority.HAUTE, "Aucun élève enregistré pour " + s.schoolName() + ".");

        rule(prefectureId, tenantId, DpeAlertType.SCHOOL_NO_TEACHERS, s.activeTeacherCount() == 0,
                DpeAlertPriority.HAUTE, "Aucun enseignant actif pour " + s.schoolName() + ".");

        rule(prefectureId, tenantId, DpeAlertType.SCHOOL_NO_CLASSES, s.classCount() == 0,
                DpeAlertPriority.MOYENNE, "Aucune classe configurée pour l'année scolaire courante à "
                        + s.schoolName() + ".");

        rule(prefectureId, tenantId, DpeAlertType.SCHOOL_INACTIVE, "INACTIVE".equals(s.status()),
                DpeAlertPriority.MOYENNE, s.schoolName() + " est actuellement suspendue ou archivée sur la plateforme.");

        boolean highRatio = s.activeTeacherCount() > 0
                && ratio(s.studentCount(), s.activeTeacherCount()).compareTo(HIGH_RATIO_THRESHOLD) > 0;
        String ratioDetail = s.activeTeacherCount() > 0 ? ratio(s.studentCount(), s.activeTeacherCount()).toPlainString() : "N/A";
        rule(prefectureId, tenantId, DpeAlertType.HIGH_STUDENT_TEACHER_RATIO, highRatio,
                DpeAlertPriority.BASSE, "Ratio élèves/enseignant élevé à " + s.schoolName() + " : " + ratioDetail
                        + " (indicatif — pas une norme officielle).");
    }

    private static BigDecimal ratio(long students, long teachers) {
        return BigDecimal.valueOf(students).divide(BigDecimal.valueOf(teachers), 1, RoundingMode.HALF_UP);
    }

    /** Crée/réouvre l'alerte si {@code conditionTrue}, la résout automatiquement sinon. */
    private void rule(Long prefectureId, Long tenantId, DpeAlertType type, boolean conditionTrue,
                       DpeAlertPriority priority, String description) {
        var existing = repository.findByPrefectureIdAndTenantIdAndType(prefectureId, tenantId, type);
        if (conditionTrue) {
            if (existing.isPresent()) {
                DpeAlert alert = existing.get();
                boolean wasResolved = alert.getStatut() == DpeAlertStatus.RESOLUE;
                alert.setDescription(description);
                alert.setPriorite(priority);
                alert.setUpdatedAt(LocalDateTime.now());
                if (wasResolved) {
                    alert.setStatut(DpeAlertStatus.NOUVELLE);
                    alert.setResolvedAt(null);
                    alert.setResolvedByUserId(null);
                }
                repository.save(alert);
            } else {
                repository.save(DpeAlert.builder()
                        .prefectureId(prefectureId).tenantId(tenantId).type(type).priorite(priority)
                        .description(description).statut(DpeAlertStatus.NOUVELLE)
                        .createdAt(LocalDateTime.now()).build());
            }
        } else {
            existing.filter(a -> a.getStatut() != DpeAlertStatus.RESOLUE).ifPresent(alert -> {
                alert.setStatut(DpeAlertStatus.RESOLUE);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setUpdatedAt(LocalDateTime.now());
                repository.save(alert);
            });
        }
    }
}
