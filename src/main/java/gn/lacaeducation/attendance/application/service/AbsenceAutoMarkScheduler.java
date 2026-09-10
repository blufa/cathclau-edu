package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.TenantListPort;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filet de sécurité : toute personne (élève ou enseignant) ayant une séance programmée aujourd'hui
 * mais aucun pointage (ni scan, ni saisie manuelle) est marquée ABSENT automatiquement — pour le
 * personnel qui oublie de cliquer « Marquer absent » manuellement. Déclenche au passage la
 * notification automatique au parent (élève) via {@code StudentAbsentEvent}. S'exécute hors contexte
 * de requête : boucle explicitement par tenant en restaurant {@link TenantContext}, comme
 * {@code BiometricPunchAttendanceListener}.
 *
 * <p>Élèves et enseignants sont vérifiés à des heures différentes (deux jobs distincts, pas un seul) :
 * les élèves à 14h, pour que le parent soit averti le jour même pendant qu'il peut encore agir
 * (récupérer l'enfant, s'organiser pour le lendemain) plutôt qu'en soirée ; les enseignants/personnel
 * restent vérifiés en fin de journée (22h), un usage RH plutôt qu'une notification à des tiers.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbsenceAutoMarkScheduler {

    private final TenantListPort tenantListPort;
    private final AcademicsDataProvider academics;
    private final StudentDataProvider students;
    private final TeacherPublicInfoProvider teachers;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final StudentCheckinRepository studentCheckinRepository;
    private final TeacherCheckinRepository teacherCheckinRepository;
    private final StudentAttendanceUseCase studentAttendance;
    private final TeacherAttendanceUseCase teacherAttendance;
    private final SessionResolver sessionResolver;

    @Scheduled(cron = "0 0 14 * * *") // 14h : assez tôt pour que le parent soit averti le jour même
    @Transactional
    public void markStudentNoShowsAbsent() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                markAbsentStudents(tenant.id(), today, now);
            } catch (Exception e) {
                log.error("Marquage auto absences élèves en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "0 0 22 * * *") // 22h : après la fin de toute activité scolaire réaliste
    @Transactional
    public void markTeacherNoShowsAbsent() {
        LocalDate today = LocalDate.now();
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                markAbsentTeachers(tenant.id(), today);
            } catch (Exception e) {
                log.error("Marquage auto absences enseignants en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void markAbsentStudents(Long tenantId, LocalDate today, LocalTime now) {
        for (ClassPublicInfo klass : academics.findClassesByTenant(tenantId)) {
            var slots = academics.findScheduleByClass(klass.id());
            // Séance déjà commencée uniquement (pas juste "un cours existe aujourd'hui") : sinon un
            // élève dont le seul cours du jour est l'après-midi serait marqué absent avant même le
            // début de son cours, à 14h.
            if (!sessionResolver.hasStartedSessionOn(slots, today, now)) continue;
            Set<Long> checkedIn = studentCheckinRepository.findByClassIdAndDate(klass.id(), today).stream()
                    .map(StudentCheckin::getStudentId).collect(Collectors.toSet());
            for (var student : students.findStudentsByClass(klass.id())) {
                if (checkedIn.contains(student.id())) continue;
                boolean alreadyRecorded = studentAttendanceRepository
                        .findByStudentIdAndClassIdAndDate(student.id(), klass.id(), today).isPresent();
                if (alreadyRecorded) continue; // déjà marqué (manuellement ou par un scan plus tôt)
                studentAttendance.markAbsent(student.id(), "Absence automatique — aucun pointage détecté");
            }
        }
    }

    private void markAbsentTeachers(Long tenantId, LocalDate today) {
        for (var teacher : teachers.listTeachers(tenantId)) {
            var slots = academics.findScheduleByTeacher(teacher.id());
            if (!sessionResolver.hasSessionOn(slots, today.getDayOfWeek())) continue;
            boolean checkedIn = !teacherCheckinRepository.findByTeacherIdAndDate(teacher.id(), today).isEmpty();
            if (checkedIn) continue;
            boolean alreadyRecorded = !teacherAttendanceRepository
                    .findByTeacherIdAndDateBetween(teacher.id(), today, today).isEmpty();
            if (alreadyRecorded) continue;
            teacherAttendance.markAbsent(teacher.id(), "Absence automatique — aucun pointage détecté");
        }
    }
}
