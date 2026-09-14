package gn.lacaeducation.attendance.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.attendance.StudentAttendanceStatsProvider.TodayAttendanceStats;
import gn.lacaeducation.attendance.application.port.out.StudentAttendanceRepository;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import gn.lacaeducation.attendance.domain.StudentAttendance;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceStatsProviderAdapterTest {

    @Mock StudentAttendanceRepository studentAttendanceRepository;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock StudentDataProvider studentDataProvider;
    @InjectMocks StudentAttendanceStatsProviderAdapter adapter;

    private ClassPublicInfo klass(Long id) {
        return new ClassPublicInfo(id, "6eme A", "6eme", "NORMALE", null, null, "COLLEGE");
    }

    private StudentPublicInfo student(Long id) {
        return new StudentPublicInfo(id, 9L, "Prenom" + id, "Nom" + id, null, null, null, null, null, null);
    }

    private StudentAttendance attendance(Long studentId, AttendanceStatus status) {
        return StudentAttendance.builder().tenantId(9L).studentId(studentId).classId(1L)
                .date(LocalDate.now()).status(status).build();
    }

    @Test
    void todayAttendanceStats_noneMarkedYet_zeroPercent() {
        when(academicsDataProvider.findClassesByTenant(9L)).thenReturn(List.of(klass(1L)));
        when(studentDataProvider.findStudentsByClass(1L)).thenReturn(List.of(student(1L), student(2L), student(3L)));
        when(studentAttendanceRepository.findByTenantIdAndDate(any(), any())).thenReturn(List.of());

        TodayAttendanceStats stats = adapter.todayAttendanceStats(9L);

        assertThat(stats.expectedCount()).isEqualTo(3);
        assertThat(stats.presentCount()).isZero();
        assertThat(stats.presentPercentage()).isEqualByComparingTo("0");
    }

    @Test
    void todayAttendanceStats_allPresent_hundredPercent() {
        when(academicsDataProvider.findClassesByTenant(9L)).thenReturn(List.of(klass(1L)));
        when(studentDataProvider.findStudentsByClass(1L)).thenReturn(List.of(student(1L), student(2L)));
        when(studentAttendanceRepository.findByTenantIdAndDate(any(), any())).thenReturn(List.of(
                attendance(1L, AttendanceStatus.PRESENT), attendance(2L, AttendanceStatus.PRESENT)));

        TodayAttendanceStats stats = adapter.todayAttendanceStats(9L);

        assertThat(stats.presentPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void todayAttendanceStats_mixedStatuses_computesCorrectCounts() {
        when(academicsDataProvider.findClassesByTenant(9L)).thenReturn(List.of(klass(1L)));
        when(studentDataProvider.findStudentsByClass(1L))
                .thenReturn(List.of(student(1L), student(2L), student(3L), student(4L)));
        when(studentAttendanceRepository.findByTenantIdAndDate(any(), any())).thenReturn(List.of(
                attendance(1L, AttendanceStatus.PRESENT),
                attendance(2L, AttendanceStatus.ABSENT),
                attendance(3L, AttendanceStatus.LATE)));
        // Élève 4 : pas encore pointé aujourd'hui — n'entre dans aucun des trois décomptes.

        TodayAttendanceStats stats = adapter.todayAttendanceStats(9L);

        assertThat(stats.expectedCount()).isEqualTo(4);
        assertThat(stats.presentCount()).isEqualTo(1);
        assertThat(stats.absentCount()).isEqualTo(1);
        assertThat(stats.lateCount()).isEqualTo(1);
        // 1 présent / 4 attendus = 25.00 %
        assertThat(stats.presentPercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    void todayAttendanceStats_noStudents_zeroPercentNoDivisionError() {
        when(academicsDataProvider.findClassesByTenant(9L)).thenReturn(List.of());
        when(studentAttendanceRepository.findByTenantIdAndDate(any(), any())).thenReturn(List.of());

        TodayAttendanceStats stats = adapter.todayAttendanceStats(9L);

        assertThat(stats.expectedCount()).isZero();
        assertThat(stats.presentPercentage()).isEqualByComparingTo("0");
    }
}
