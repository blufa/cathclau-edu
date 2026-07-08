package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import gn.lacaeducation.grading.domain.AssessmentType;

public interface AssessmentSessionSettingsUseCase {
    record UpdateSettingsCommand(Integer quizMinutes, Integer homeworkMinutes,
                                 Integer examMinutes, Integer finalMinutes) {}

    /** Returns the tenant's configured session durations, or the defaults if none has been set. */
    AssessmentSessionSettings getForTenant(Long tenantId);

    AssessmentSessionSettings update(Long tenantId, UpdateSettingsCommand command);

    /** Resolved session duration (minutes) for the given tenant and assessment type. */
    int sessionMinutesFor(Long tenantId, AssessmentType type);
}
