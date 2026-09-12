package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AttendanceSettingsPersistenceAdapter implements AttendanceSettingsRepository {

    private final AttendanceSettingsJpaRepository repo;

    @Override
    public Optional<AttendanceSettings> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public AttendanceSettings save(AttendanceSettings s) {
        AttendanceSettingsJpaEntity e = repo.findByTenantId(s.getTenantId())
                .orElseGet(AttendanceSettingsJpaEntity::new);
        e.setTenantId(s.getTenantId());
        e.setTransitionMarginMinutes(s.transitionMarginMinutesOrDefault());
        e.setStudentTransitionMarginMinutes(s.studentTransitionMarginMinutesOrDefault());
        e.setEmployeeCheckInTime(s.employeeCheckInTimeOrDefault());
        e.setEmployeeCheckOutTime(s.employeeCheckOutTimeOrDefault());
        return toDomain(repo.save(e));
    }

    private AttendanceSettings toDomain(AttendanceSettingsJpaEntity e) {
        return AttendanceSettings.builder().id(e.getId()).tenantId(e.getTenantId())
                .transitionMarginMinutes(e.getTransitionMarginMinutes())
                .studentTransitionMarginMinutes(e.getStudentTransitionMarginMinutes())
                .employeeCheckInTime(e.getEmployeeCheckInTime()).employeeCheckOutTime(e.getEmployeeCheckOutTime()).build();
    }
}
