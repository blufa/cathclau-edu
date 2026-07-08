package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.AssessmentSessionSettingsUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentSessionSettingsRepository;
import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import gn.lacaeducation.grading.domain.AssessmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssessmentSessionSettingsService implements AssessmentSessionSettingsUseCase {

    /** Garde-fou : une séance dure entre 5 minutes et 8 heures. */
    private static final int MIN_MINUTES = 5;
    private static final int MAX_MINUTES = 480;

    private final AssessmentSessionSettingsRepository repo;

    @Override
    @Transactional(readOnly = true)
    public AssessmentSessionSettings getForTenant(Long tenantId) {
        AssessmentSessionSettings defaults = AssessmentSessionSettings.defaults();
        defaults.setTenantId(tenantId);
        return repo.findByTenantId(tenantId).orElse(defaults);
    }

    @Override
    @Transactional(readOnly = true)
    public int sessionMinutesFor(Long tenantId, AssessmentType type) {
        return getForTenant(tenantId).minutesFor(type);
    }

    @Override
    public AssessmentSessionSettings update(Long tenantId, UpdateSettingsCommand c) {
        validate(c.quizMinutes()); validate(c.homeworkMinutes());
        validate(c.examMinutes()); validate(c.finalMinutes());
        AssessmentSessionSettings defaults = AssessmentSessionSettings.defaults();
        return repo.save(AssessmentSessionSettings.builder()
                .tenantId(tenantId)
                .quizMinutes(c.quizMinutes() != null ? c.quizMinutes() : defaults.getQuizMinutes())
                .homeworkMinutes(c.homeworkMinutes() != null ? c.homeworkMinutes() : defaults.getHomeworkMinutes())
                .examMinutes(c.examMinutes() != null ? c.examMinutes() : defaults.getExamMinutes())
                .finalMinutes(c.finalMinutes() != null ? c.finalMinutes() : defaults.getFinalMinutes())
                .build());
    }

    private void validate(Integer minutes) {
        if (minutes != null && (minutes < MIN_MINUTES || minutes > MAX_MINUTES)) {
            throw new BusinessRuleException(
                    "La durée d'une séance doit être comprise entre " + MIN_MINUTES + " et " + MAX_MINUTES + " minutes.");
        }
    }
}
