package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.PrimaryTimetableSettings;

import java.time.LocalTime;

public interface PrimaryTimetableSettingsUseCase {

    PrimaryTimetableSettings getSettings(Long tenantId);

    PrimaryTimetableSettings updateGrid(Long tenantId, LocalTime gridStartTime, LocalTime gridEndTime,
                                         Integer bandMinutes);
}
