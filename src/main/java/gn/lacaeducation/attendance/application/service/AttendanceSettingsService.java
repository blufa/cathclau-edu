package gn.lacaeducation.attendance.application.service;

import gn.lacaeducation.attendance.application.port.in.AttendanceSettingsUseCase;
import gn.lacaeducation.attendance.application.port.out.AttendanceSettingsRepository;
import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.domain.AttendanceSettings;
import gn.lacaeducation.attendance.domain.RecessWindow;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceSettingsService implements AttendanceSettingsUseCase {

    private final AttendanceSettingsRepository settingsRepo;
    private final RecessWindowRepository recessRepo;

    @Override
    @Transactional(readOnly = true)
    public AttendanceSettings getSettings(Long tenantId) {
        AttendanceSettings defaults = AttendanceSettings.defaults();
        defaults.setTenantId(tenantId);
        return settingsRepo.findByTenantId(tenantId).orElse(defaults);
    }

    @Override
    public AttendanceSettings updateTransitionMargin(Long tenantId, Integer transitionMarginMinutes) {
        if (transitionMarginMinutes != null && (transitionMarginMinutes < 0 || transitionMarginMinutes > 60)) {
            throw new BusinessRuleException("La marge de transition doit être comprise entre 0 et 60 minutes.");
        }
        // Repart des réglages actuels (pas d'un builder vierge) : sinon les horaires du personnel
        // configurés par ailleurs seraient silencieusement réinitialisés à leur valeur par défaut.
        AttendanceSettings current = getSettings(tenantId);
        current.setTransitionMarginMinutes(transitionMarginMinutes);
        return settingsRepo.save(current);
    }

    @Override
    public AttendanceSettings updateStudentTransitionMargin(Long tenantId, Integer studentTransitionMarginMinutes) {
        if (studentTransitionMarginMinutes != null && (studentTransitionMarginMinutes < 0 || studentTransitionMarginMinutes > 60)) {
            throw new BusinessRuleException("La marge de tolérance élève doit être comprise entre 0 et 60 minutes.");
        }
        AttendanceSettings current = getSettings(tenantId);
        current.setStudentTransitionMarginMinutes(studentTransitionMarginMinutes);
        return settingsRepo.save(current);
    }

    @Override
    public AttendanceSettings updateEmployeeHours(Long tenantId, LocalTime checkInTime, LocalTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null || !checkInTime.isBefore(checkOutTime)) {
            throw new BusinessRuleException("L'heure d'entrée du personnel doit être antérieure à l'heure de sortie.");
        }
        AttendanceSettings current = getSettings(tenantId);
        current.setEmployeeCheckInTime(checkInTime);
        current.setEmployeeCheckOutTime(checkOutTime);
        return settingsRepo.save(current);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecessWindow> listRecessWindows(Long tenantId) {
        return recessRepo.findByTenantId(tenantId);
    }

    @Override
    public RecessWindow addRecessWindow(Long tenantId, String label, LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessRuleException("La fenêtre de récréation doit avoir une heure de début antérieure à l'heure de fin.");
        }
        return recessRepo.save(RecessWindow.builder()
                .tenantId(tenantId).label(label).startTime(startTime).endTime(endTime).build());
    }

    @Override
    public void deleteRecessWindow(Long tenantId, Long id) {
        boolean owned = recessRepo.findByTenantId(tenantId).stream().anyMatch(w -> w.getId().equals(id));
        if (!owned) throw new BusinessRuleException("Fenêtre de récréation introuvable pour cet établissement.");
        recessRepo.deleteById(id);
    }
}
