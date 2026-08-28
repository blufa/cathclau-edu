package gn.lacaeducation.attendance.adapters.in.web;

import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Pointage de présence par scan de la carte scolaire (QR), à l'entrée et à la sortie de chaque
 * séance. Le QR encode l'identifiant de la personne ; le statut (présent/retard) et la durée
 * réalisée sont calculés automatiquement à partir de l'emploi du temps — l'opérateur choisit
 * seulement le sens du scan (entrée/sortie). Une absence ne peut jamais résulter d'un scan (une
 * personne absente ne scanne pas) : voir les endpoints "mark-absent" dédiés.
 */
@Tag(name = "Attendance Scan", description = "Pointage présence par scan de la carte scolaire (élève et enseignant)")
@RestController
@RequestMapping("/api/v1/attendance/scan")
@RequiredArgsConstructor
public class AttendanceScanController {

    private final StudentAttendanceUseCase studentAttendance;
    private final TeacherAttendanceUseCase teacherAttendance;

    public enum SubjectType { STUDENT, TEACHER }
    public enum ScanDirection { ENTRY, EXIT }
    public enum AttendanceOutcome { PRESENT, LATE }

    public record ScanRequest(SubjectType subjectType, Long subjectId, ScanDirection direction) {}

    public record ScanResponse(
            String subjectType, Long subjectId, String name, ScanDirection direction,
            AttendanceOutcome outcome,      // entrée uniquement
            Integer lateMinutes,            // entrée uniquement
            LocalTime checkInTime,
            LocalTime checkOutTime,         // sortie uniquement
            Integer realizedMinutes,        // sortie : checkOutTime - checkInTime
            Integer scheduledMinutes,       // sortie : sessionEnd - sessionStart
            Boolean requiresPickupInfo,     // sortie élève uniquement : classe de cycle maternelle/primaire
            LocalDate date) {}

    /** Vue commune pour construire une {@link ScanResponse}, quel que soit le type de pointage
     * (élève ou enseignant — deux classes structurellement identiques mais sans interface commune). */
    private record CheckinView(LocalTime sessionStart, LocalTime sessionEnd, LocalTime checkInTime, LocalTime checkOutTime, Integer lateMinutes) {}

    // L'enseignant ne doit JAMAIS pouvoir scanner (ni sa propre présence, ni celle des élèves) —
    // retiré volontairement de la liste des rôles autorisés. Le personnel (admin/direction/secrétariat)
    // et l'élève chef de classe désigné restent autorisés. TODO : ajouter le rôle SURVEILLANT ici une
    // fois créé (rôle prévu pour la surveillance/scan, distinct de TEACHER).
    @Operation(summary = "Pointer une entrée ou une sortie de séance à partir d'un scan de carte "
            + "(élève ou enseignant) — statut et durée réalisée calculés automatiquement à partir de "
            + "l'emploi du temps. Un élève ne peut scanner que s'il est le chef de classe désigné de "
            + "la classe scannée. Les enseignants ne peuvent jamais scanner (ni eux-mêmes ni les élèves).")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','STUDENT')")
    @PostMapping
    public ScanResponse scan(@RequestBody ScanRequest r, @AuthenticationPrincipal Jwt jwt) {
        // Évite une erreur SQL brute (contrainte tenant_id NOT NULL) si la session n'a pas de tenant
        // résolu (ex. jeton invalide/expiré, compte sans établissement) — message clair à la place.
        if (TenantContext.get() == null) {
            throw new BusinessRuleException("Session sans établissement associé : reconnectez-vous et réessayez le scan.");
        }
        if (r.subjectId() == null) throw new BusinessRuleException("Identifiant manquant dans le scan");
        if (r.direction() == null) throw new BusinessRuleException("Sens du scan manquant (entrée/sortie)");
        SubjectType type = r.subjectType() != null ? r.subjectType() : SubjectType.STUDENT;

        if (type == SubjectType.TEACHER) {
            if (r.direction() == ScanDirection.ENTRY) {
                TeacherCheckin c = idempotent(() -> teacherAttendance.recordEntry(r.subjectId()));
                return response("TEACHER", r.subjectId(), null, r.direction(), toView(c), null);
            }
            TeacherCheckin c = idempotent(() -> teacherAttendance.recordExit(r.subjectId()));
            return response("TEACHER", r.subjectId(), null, r.direction(), toView(c), null);
        }

        Long scannerUserId = null;
        try { scannerUserId = jwt != null ? Long.parseLong(jwt.getSubject()) : null; } catch (NumberFormatException ignored) { /* SUPER_ADMIN or non-numeric subject */ }
        Long finalScannerUserId = scannerUserId;
        String name = studentAttendance.studentName(r.subjectId());
        if (r.direction() == ScanDirection.ENTRY) {
            StudentCheckin c = idempotent(() -> studentAttendance.recordEntry(r.subjectId(), finalScannerUserId));
            return response("STUDENT", r.subjectId(), name, r.direction(), toView(c), null);
        }
        StudentCheckin c = idempotent(() -> studentAttendance.recordExit(r.subjectId(), finalScannerUserId));
        // Badge de retrait : uniquement pour un élève en sortie d'une classe de cycle maternelle/primaire.
        boolean requiresPickup = studentAttendance.requiresPickup(c.getClassId());
        return response("STUDENT", r.subjectId(), name, r.direction(), toView(c), requiresPickup);
    }

    private static CheckinView toView(TeacherCheckin c) {
        return new CheckinView(c.getSessionStart(), c.getSessionEnd(), c.getCheckInTime(), c.getCheckOutTime(), c.getLateMinutes());
    }

    private static CheckinView toView(StudentCheckin c) {
        return new CheckinView(c.getSessionStart(), c.getSessionEnd(), c.getCheckInTime(), c.getCheckOutTime(), c.getLateMinutes());
    }

    private static ScanResponse response(String subjectType, Long subjectId, String name, ScanDirection direction, CheckinView v, Boolean requiresPickupInfo) {
        if (direction == ScanDirection.ENTRY) {
            int late = v.lateMinutes() != null ? v.lateMinutes() : 0;
            AttendanceOutcome outcome = late > 0 ? AttendanceOutcome.LATE : AttendanceOutcome.PRESENT;
            return new ScanResponse(subjectType, subjectId, name, direction, outcome, late, v.checkInTime(),
                    null, null, null, null, LocalDate.now());
        }
        Integer realized = (v.checkInTime() != null && v.checkOutTime() != null)
                ? (int) Duration.between(v.checkInTime(), v.checkOutTime()).toMinutes() : null;
        Integer scheduled = (v.sessionStart() != null && v.sessionEnd() != null)
                ? (int) Duration.between(v.sessionStart(), v.sessionEnd()).toMinutes() : null;
        return new ScanResponse(subjectType, subjectId, name, direction, null, null, v.checkInTime(),
                v.checkOutTime(), realized, scheduled, requiresPickupInfo, LocalDate.now());
    }

    /**
     * Scans quasi simultanés du même sujet → deux transactions insèrent en parallèle et l'une viole
     * l'unicité (par séance). On réessaie une fois : la 2e tentative trouve la ligne déjà créée et la
     * met à jour, rendant le pointage réellement idempotent (au lieu d'un 409).
     */
    private <T> T idempotent(Supplier<T> op) {
        try {
            return op.get();
        } catch (DataIntegrityViolationException e) {
            return op.get();
        }
    }
}
