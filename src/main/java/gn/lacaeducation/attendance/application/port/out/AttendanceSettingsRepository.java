package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.AttendanceSettings;

import java.util.Optional;

public interface AttendanceSettingsRepository {
    Optional<AttendanceSettings> findByTenantId(Long tenantId);
    AttendanceSettings save(AttendanceSettings settings);
}
