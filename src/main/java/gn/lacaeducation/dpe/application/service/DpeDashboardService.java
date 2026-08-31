package gn.lacaeducation.dpe.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase;
import gn.lacaeducation.platform.StudentCountPort;
import gn.lacaeducation.platform.TeacherCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DpeDashboardService implements DpeDashboardUseCase {

    private final TenantInfoProvider tenantInfoProvider;
    private final StudentCountPort studentCountPort;
    private final TeacherCountPort teacherCountPort;
    private final AcademicsDataProvider academicsDataProvider;

    @Override
    public DashboardOverview overview(Set<Long> tenantIds) {
        List<SchoolSummary> schools = schools(tenantIds);
        long students = schools.stream().mapToLong(SchoolSummary::studentCount).sum();
        long teachers = schools.stream().mapToLong(SchoolSummary::teacherCount).sum();
        long classes = schools.stream().mapToLong(SchoolSummary::classCount).sum();
        BigDecimal ratio = teachers > 0
                ? BigDecimal.valueOf(students).divide(BigDecimal.valueOf(teachers), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new DashboardOverview(schools.size(), students, teachers, classes, ratio, schools);
    }

    @Override
    public List<SchoolSummary> schools(Set<Long> tenantIds) {
        return tenantIds.stream()
                .map(this::summaryOf)
                .sorted((a, b) -> a.schoolName().compareToIgnoreCase(b.schoolName()))
                .toList();
    }

    @Override
    public SchoolSummary schoolDetail(Set<Long> allowedTenantIds, Long tenantId) {
        if (!allowedTenantIds.contains(tenantId)) {
            throw new AccessDeniedException("Cette école n'appartient pas à votre périmètre.");
        }
        return summaryOf(tenantId);
    }

    private SchoolSummary summaryOf(Long tenantId) {
        var info = tenantInfoProvider.findTenantInfo(tenantId);
        String schoolName = info.map(i -> i.schoolName()).orElse("École #" + tenantId);
        String city = info.map(i -> i.city()).orElse(null);
        boolean active = tenantInfoProvider.isTenantActive(tenantId);

        long studentCount = studentCountPort.countByTenant(tenantId);
        long teacherCount = teacherCountPort.countByTenant(tenantId);
        long activeTeacherCount = teacherCountPort.countActiveByTenant(tenantId);

        // Classes de l'année scolaire courante uniquement (une école accumule des classes d'années
        // révolues au fil du temps — les compter toutes fausserait l'effectif affiché).
        long classCount = academicsDataProvider.currentAcademicYear(tenantId)
                .map(year -> academicsDataProvider.countClassesByTenantAndYear(tenantId, year.id()))
                .orElse(0L);

        return new SchoolSummary(tenantId, schoolName, city, active ? "ACTIVE" : "INACTIVE",
                studentCount, teacherCount, activeTeacherCount, classCount);
    }
}
