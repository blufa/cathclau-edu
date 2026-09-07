package gn.lacaeducation.dpe.application.service;

import gn.lacaeducation.academics.AcademicYearPublicInfo;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.platform.StudentCountPort;
import gn.lacaeducation.platform.TeacherCountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DpeDashboardServiceTest {

    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock StudentCountPort studentCountPort;
    @Mock TeacherCountPort teacherCountPort;
    @Mock AcademicsDataProvider academicsDataProvider;

    DpeDashboardService service() {
        return new DpeDashboardService(tenantInfoProvider, studentCountPort, teacherCountPort, academicsDataProvider);
    }

    private void stubSchool(Long tenantId, String name, long students, long teachers, long activeTeachers, long classes) {
        when(tenantInfoProvider.findTenantInfo(tenantId)).thenReturn(Optional.of(
                new TenantPublicInfo(tenantId, name, null, null, null, null, "Coyah", "Guinée",
                        null, null, null, null, null, null, null, null, null)));
        when(tenantInfoProvider.isTenantActive(tenantId)).thenReturn(true);
        when(studentCountPort.countByTenant(tenantId)).thenReturn(students);
        when(teacherCountPort.countByTenant(tenantId)).thenReturn(teachers);
        when(teacherCountPort.countActiveByTenant(tenantId)).thenReturn(activeTeachers);
        when(academicsDataProvider.currentAcademicYear(tenantId)).thenReturn(
                Optional.of(new AcademicYearPublicInfo(99L, "2025-2026", LocalDate.now(), LocalDate.now())));
        when(academicsDataProvider.countClassesByTenantAndYear(tenantId, 99L)).thenReturn(classes);
    }

    @Test
    void overview_aggregatesAcrossAllTenantsInScope() {
        stubSchool(1L, "École A", 200, 10, 9, 8);
        stubSchool(2L, "École B", 150, 8, 8, 6);

        var overview = service().overview(Set.of(1L, 2L));

        assertThat(overview.schoolCount()).isEqualTo(2);
        assertThat(overview.studentCount()).isEqualTo(350);
        assertThat(overview.teacherCount()).isEqualTo(18);
        assertThat(overview.classCount()).isEqualTo(14);
        assertThat(overview.studentTeacherRatio()).isEqualByComparingTo("19.4");
    }

    @Test
    void overview_ratioIsZero_whenNoTeachers() {
        stubSchool(1L, "École A", 50, 0, 0, 3);

        var overview = service().overview(Set.of(1L));

        assertThat(overview.studentTeacherRatio()).isEqualByComparingTo("0");
    }

    @Test
    void schools_sortsByNameAlphabetically() {
        stubSchool(1L, "Zamora", 10, 1, 1, 1);
        stubSchool(2L, "Alpha", 10, 1, 1, 1);

        var schools = service().schools(Set.of(1L, 2L));

        assertThat(schools).extracting(s -> s.schoolName()).containsExactly("Alpha", "Zamora");
    }

    @Test
    void schoolDetail_deniesAccess_whenTenantOutsideScope() {
        assertThatThrownBy(() -> service().schoolDetail(Set.of(1L, 2L), 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void schoolDetail_returnsSummary_whenTenantInScope() {
        stubSchool(1L, "École A", 200, 10, 9, 8);

        var detail = service().schoolDetail(Set.of(1L, 2L), 1L);

        assertThat(detail.schoolName()).isEqualTo("École A");
        assertThat(detail.studentCount()).isEqualTo(200);
    }

    @Test
    void summaryOf_classCountIsZero_whenNoCurrentAcademicYear() {
        when(tenantInfoProvider.findTenantInfo(1L)).thenReturn(Optional.of(
                new TenantPublicInfo(1L, "École A", null, null, null, null, "Coyah", "Guinée",
                        null, null, null, null, null, null, null, null, null)));
        when(tenantInfoProvider.isTenantActive(1L)).thenReturn(true);
        when(studentCountPort.countByTenant(1L)).thenReturn(10L);
        when(teacherCountPort.countByTenant(1L)).thenReturn(2L);
        when(teacherCountPort.countActiveByTenant(1L)).thenReturn(2L);
        when(academicsDataProvider.currentAcademicYear(1L)).thenReturn(Optional.empty());

        var detail = service().schoolDetail(Set.of(1L), 1L);

        assertThat(detail.classCount()).isZero();
    }
}
