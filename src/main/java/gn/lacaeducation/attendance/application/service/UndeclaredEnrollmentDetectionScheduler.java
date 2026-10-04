package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.event.UndeclaredAttendanceEvent;
import gn.lacaeducation.attendance.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Détecteur B1 (§retour fondateurs) : un élève présent/en retard aujourd'hui mais sans inscription
 * active déclarée est un signal direct du vol décrit par les fondateurs — élève reçu et payé en
 * main à main, jamais déclaré dans le système pour ne laisser aucune trace. Tourne après le job
 * d'absence automatique (14h) pour que les présences du jour soient déjà saisies. Même boucle par
 * tenant que {@link AbsenceAutoMarkScheduler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UndeclaredEnrollmentDetectionScheduler {

    private final TenantListPort tenantListPort;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 30 14 * * *")
    @Transactional
    public void detectUndeclaredAttendance() {
        LocalDate today = LocalDate.now();
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                detectForTenant(tenant.id(), today);
            } catch (Exception e) {
                log.error("Détection présence sans inscription en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void detectForTenant(Long tenantId, LocalDate today) {
        Set<Long> presentStudentIds = studentAttendanceRepository.findByTenantIdAndDate(tenantId, today).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .map(StudentAttendance::getStudentId)
                .collect(Collectors.toSet());
        for (Long studentId : presentStudentIds) {
            if (!enrollmentQueryPort.hasActiveEnrollment(studentId)) {
                eventPublisher.publishEvent(new UndeclaredAttendanceEvent(tenantId, studentId, today));
            }
        }
    }
}
