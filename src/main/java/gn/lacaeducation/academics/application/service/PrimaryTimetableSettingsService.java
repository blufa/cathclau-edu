package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.PrimaryTimetableSettingsUseCase;
import gn.lacaeducation.academics.application.port.out.PrimaryTimetableSettingsRepository;
import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PrimaryTimetableSettingsService implements PrimaryTimetableSettingsUseCase {

    private final PrimaryTimetableSettingsRepository settingsRepo;

    @Override
    @Transactional(readOnly = true)
    public PrimaryTimetableSettings getSettings(Long tenantId) {
        PrimaryTimetableSettings defaults = PrimaryTimetableSettings.defaults();
        defaults.setTenantId(tenantId);
        return settingsRepo.findByTenantId(tenantId).orElse(defaults);
    }

    @Override
    public PrimaryTimetableSettings updateGrid(Long tenantId, LocalTime gridStartTime, LocalTime gridEndTime,
                                                 Integer bandMinutes) {
        if (gridStartTime == null || gridEndTime == null || !gridStartTime.isBefore(gridEndTime)) {
            throw new BusinessRuleException("L'heure de début de la grille doit être antérieure à l'heure de fin.");
        }
        if (bandMinutes != null && (bandMinutes < 15 || bandMinutes > 240)) {
            throw new BusinessRuleException("La durée des créneaux doit être comprise entre 15 et 240 minutes.");
        }
        // Repart des réglages actuels (pas d'un builder vierge), même convention que
        // AttendanceSettingsService#updateTransitionMargin.
        PrimaryTimetableSettings current = getSettings(tenantId);
        current.setGridStartTime(gridStartTime);
        current.setGridEndTime(gridEndTime);
        current.setBandMinutes(bandMinutes);
        return settingsRepo.save(current);
    }
}
