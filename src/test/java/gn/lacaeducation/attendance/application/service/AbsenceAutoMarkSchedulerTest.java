package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.attendance.application.port.in.StudentAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.in.TeacherAttendanceUseCase;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.StudentCheckinRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherAttendanceRepository;
import gn.lacaeducation.attendance.application.port.out.TeacherCheckinRepository;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.attendance.domain.StudentCheckin;
import gn.lacaeducation.attendance.domain.TeacherCheckin;
import gn.lacaeducation.platform.TenantListPort;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceAutoMarkSchedulerTest {

    @Mock TenantListPort tenantListPort;
    @Mock AcademicsDataProvider academics;
    @Mock StudentDataProvider students;
    @Mock TeacherPublicInfoProvider teachers;
    @Mock StudentAttendanceRepository studentAttendanceRepository;
    @Mock TeacherAttendanceRepository teacherAttendanceRepository;
    @Mock StudentCheckinRepository studentCheckinRepository;
    @Mock TeacherCheckinRepository teacherCheckinRepository;
    @Mock StudentAttendanceUseCase studentAttendance;
    @Mock TeacherAttendanceUseCase teacherAttendance;
    @Mock SessionResolver sessionResolver;
    @InjectMocks AbsenceAutoMarkScheduler scheduler;

    private static final ClassPublicInfo CLASS = new ClassPublicInfo(20L, "6e A", null, "NORMALE", null, null, null);
    private static StudentPublicInfo student(long id) {
        return new StudentPublicInfo(id, 1L, "Prenom", "Nom", null, null, "M-" + id, null, null, "F");
    }
    private static TeacherInfo teacher(long id) {
        return new TeacherInfo(id, "Prenom", "Nom", "E-" + id, null, null, null);
    }

    @Test
    void markStudentNoShowsAbsent_studentWithStartedSessionAndNoCheckin_getsMarkedAbsent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(academics.findClassesByTenant(1L)).thenReturn(List.of(CLASS));
        when(academics.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.hasStartedSessionOn(any(), any(), any())).thenReturn(true);
        when(studentCheckinRepository.findByClassIdAndDate(eq(20L), any())).thenReturn(List.of());
        when(students.findStudentsByClass(20L)).thenReturn(List.of(student(10L)));
        when(studentAttendanceRepository.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());

        scheduler.markStudentNoShowsAbsent();

        verify(studentAttendance).markAbsent(eq(10L), any());
    }

    @Test
    void markStudentNoShowsAbsent_studentWithCheckin_notMarkedAbsent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(academics.findClassesByTenant(1L)).thenReturn(List.of(CLASS));
        when(academics.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.hasStartedSessionOn(any(), any(), any())).thenReturn(true);
        StudentCheckin checkin = StudentCheckin.builder().studentId(10L).build();
        when(studentCheckinRepository.findByClassIdAndDate(eq(20L), any())).thenReturn(List.of(checkin));
        when(students.findStudentsByClass(20L)).thenReturn(List.of(student(10L)));

        scheduler.markStudentNoShowsAbsent();

        verify(studentAttendance, never()).markAbsent(anyLong(), any());
    }

    @Test
    void markStudentNoShowsAbsent_studentAlreadyRecorded_skipped() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(academics.findClassesByTenant(1L)).thenReturn(List.of(CLASS));
        when(academics.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.hasStartedSessionOn(any(), any(), any())).thenReturn(true);
        when(studentCheckinRepository.findByClassIdAndDate(eq(20L), any())).thenReturn(List.of());
        when(students.findStudentsByClass(20L)).thenReturn(List.of(student(10L)));
        when(studentAttendanceRepository.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any()))
                .thenReturn(Optional.of(StudentAttendance.builder().id(1L).build()));

        scheduler.markStudentNoShowsAbsent();

        verify(studentAttendance, never()).markAbsent(anyLong(), any());
    }

    @Test
    void markStudentNoShowsAbsent_noStartedSessionYet_skipped() {
        // Ex. un élève dont le seul cours du jour est l'après-midi : à 14h, ce cours n'a pas encore
        // commencé, il ne doit pas être marqué absent par anticipation.
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(academics.findClassesByTenant(1L)).thenReturn(List.of(CLASS));
        when(academics.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.hasStartedSessionOn(any(), any(), any())).thenReturn(false);

        scheduler.markStudentNoShowsAbsent();

        verify(studentAttendance, never()).markAbsent(anyLong(), any());
        verify(studentCheckinRepository, never()).findByClassIdAndDate(any(), any());
    }

    @Test
    void markStudentNoShowsAbsent_loopsAcrossMultipleTenants_continuesAfterOneFails() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(
                new TenantListPort.TenantRef(1L), new TenantListPort.TenantRef(2L)));
        when(academics.findClassesByTenant(1L)).thenThrow(new RuntimeException("boom"));
        when(academics.findClassesByTenant(2L)).thenReturn(List.of(CLASS));
        when(academics.findScheduleByClass(20L)).thenReturn(List.of());
        when(sessionResolver.hasStartedSessionOn(any(), any(), any())).thenReturn(true);
        when(studentCheckinRepository.findByClassIdAndDate(eq(20L), any())).thenReturn(List.of());
        when(students.findStudentsByClass(20L)).thenReturn(List.of(student(10L)));
        when(studentAttendanceRepository.findByStudentIdAndClassIdAndDate(eq(10L), eq(20L), any())).thenReturn(Optional.empty());

        scheduler.markStudentNoShowsAbsent();

        verify(studentAttendance, times(1)).markAbsent(eq(10L), any());
    }

    @Test
    void markTeacherNoShowsAbsent_teacherWithSessionAndNoCheckin_getsMarkedAbsent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(teachers.listTeachers(1L)).thenReturn(List.of(teacher(3L)));
        when(academics.findScheduleByTeacher(3L)).thenReturn(List.of());
        when(sessionResolver.hasSessionOn(any(), any())).thenReturn(true);
        when(teacherCheckinRepository.findByTeacherIdAndDate(eq(3L), any())).thenReturn(List.of());
        when(teacherAttendanceRepository.findByTeacherIdAndDateBetween(eq(3L), any(), any())).thenReturn(List.of());

        scheduler.markTeacherNoShowsAbsent();

        verify(teacherAttendance).markAbsent(eq(3L), any());
    }

    @Test
    void markTeacherNoShowsAbsent_teacherWithCheckin_notMarkedAbsent() {
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
        when(teachers.listTeachers(1L)).thenReturn(List.of(teacher(3L)));
        when(academics.findScheduleByTeacher(3L)).thenReturn(List.of());
        when(sessionResolver.hasSessionOn(any(), any())).thenReturn(true);
        when(teacherCheckinRepository.findByTeacherIdAndDate(eq(3L), any()))
                .thenReturn(List.of(TeacherCheckin.builder().teacherId(3L).build()));

        scheduler.markTeacherNoShowsAbsent();

        verify(teacherAttendance, never()).markAbsent(anyLong(), any());
    }
}
