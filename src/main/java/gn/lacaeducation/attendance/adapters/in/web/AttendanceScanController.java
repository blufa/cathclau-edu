package gn.lacaeducation.attendance.adapters.in.web;

import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.common.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Pointage de présence par scan de la carte scolaire (QR). Le QR encode l'identifiant de l'élève ;
 * l'opérateur choisit le statut (présent / absent / retard). Fonctionne aussi pour un enseignant
 * (statut présent / absent, le retard étant ramené à présent côté RH). Idempotent par jour.
 */
@Tag(name = "Attendance Scan", description = "Pointage présence par scan de la carte scolaire (élève et enseignant)")
@RestController
@RequestMapping("/api/v1/attendance/scan")
@RequiredArgsConstructor
public class AttendanceScanController {

    private final StudentAttendanceUseCase studentAttendance;
    private final TeacherAttendanceUseCase teacherAttendance;

    public enum SubjectType { STUDENT, TEACHER }
    public record ScanRequest(SubjectType subjectType, Long subjectId, AttendanceStatus status) {}
    public record ScanResponse(String subjectType, Long subjectId, String name, AttendanceStatus status, LocalDate date) {}

    @Operation(summary = "Pointer une présence à partir d'un scan de carte (élève ou enseignant)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY','TEACHER')")
    @PostMapping
    public ScanResponse scan(@RequestBody ScanRequest r) {
        if (r.subjectId() == null) throw new BusinessRuleException("Identifiant manquant dans le scan");
        AttendanceStatus status = r.status() != null ? r.status() : AttendanceStatus.PRESENT;
        SubjectType type = r.subjectType() != null ? r.subjectType() : SubjectType.STUDENT;
        if (type == SubjectType.TEACHER) {
            TeacherAttendanceStatus ts = status == AttendanceStatus.ABSENT
                    ? TeacherAttendanceStatus.ABSENT : TeacherAttendanceStatus.PRESENT;
            teacherAttendance.recordByScan(r.subjectId(), ts);
            return new ScanResponse("TEACHER", r.subjectId(), null,
                    ts == TeacherAttendanceStatus.ABSENT ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT, LocalDate.now());
        }
        var saved = studentAttendance.recordByScan(r.subjectId(), status);
        return new ScanResponse("STUDENT", r.subjectId(), studentAttendance.studentName(r.subjectId()),
                saved.getStatus(), saved.getDate());
    }
}
