package gn.lacaeducation.attendance.application.port.in;

import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.RecessWindow;

import java.time.LocalTime;
import java.util.List;

public interface AttendanceSettingsUseCase {
    /** Réglage du tenant, ou les valeurs par défaut (marge de 5 min) si non configuré. */
    AttendanceSettings getSettings(Long tenantId);
    AttendanceSettings updateTransitionMargin(Long tenantId, Integer transitionMarginMinutes);
    /** Horaire de référence du personnel non-enseignant (un seul horaire pour tout le personnel). */
    AttendanceSettings updateEmployeeHours(Long tenantId, LocalTime checkInTime, LocalTime checkOutTime);
    List<RecessWindow> listRecessWindows(Long tenantId);
    RecessWindow addRecessWindow(Long tenantId, String label, LocalTime startTime, LocalTime endTime);
    void deleteRecessWindow(Long tenantId, Long id);
}
