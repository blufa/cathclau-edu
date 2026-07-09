package gn.lacaeducation.attendance.application.listener;

import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase.RecordAttendanceCommand;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase.RecordTeacherAttendanceCommand;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import gn.lacaeducation.biometric.application.event.BiometricPunchRecorded;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * Branche la biométrie sur les présences : un pointage rattaché à une personne (sauf une « sortie »)
 * crée sa présence du jour, de façon idempotente (une seule par jour). Pour un élève, sa classe est
 * résolue via sa dernière inscription validée (StudentDataProvider). Le tenant est restauré depuis
 * l'événement car le listener s'exécute hors contexte web.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BiometricPunchAttendanceListener {

    private final TeacherAttendanceUseCase teacherAttendance;
    private final StudentAttendanceUseCase studentAttendance;
    private final StudentDataProvider studentDataProvider;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPunch(BiometricPunchRecorded e) {
        if (e.subjectId() == null || e.punchTime() == null) return;
        if ("OUT".equals(e.direction())) return; // une sortie ne crée pas la présence
        TenantContext.set(e.tenantId());
        try {
            LocalDate date = e.punchTime().toLocalDate();
            if ("TEACHER".equals(e.subjectType())) {
                if (teacherAttendance.findByTeacher(e.subjectId(), date, date).isEmpty()) {
                    teacherAttendance.record(new RecordTeacherAttendanceCommand(
                            e.subjectId(), date, TeacherAttendanceStatus.PRESENT, "Pointage biométrique"));
                    log.info("Présence enseignant {} créée le {} (biométrie)", e.subjectId(), date);
                }
            } else if ("STUDENT".equals(e.subjectType())) {
                Long classId = studentDataProvider.findCurrentClassId(e.subjectId()).orElse(null);
                if (classId == null) {
                    log.warn("Élève {} sans classe courante : présence biométrique non créée", e.subjectId());
                    return;
                }
                if (studentAttendance.findByStudent(e.subjectId(), date, date).isEmpty()) {
                    studentAttendance.record(new RecordAttendanceCommand(
                            e.subjectId(), classId, date, AttendanceStatus.PRESENT, "Pointage biométrique"));
                    log.info("Présence élève {} (classe {}) créée le {} (biométrie)", e.subjectId(), classId, date);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Présence biométrique non créée ({} {}) : {}", e.subjectType(), e.subjectId(), ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
