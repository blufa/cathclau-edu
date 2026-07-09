package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.AssessmentSessionSettingsRepository;
import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AssessmentSessionSettingsPersistenceAdapter implements AssessmentSessionSettingsRepository {

    private final AssessmentSessionSettingsJpaRepository repo;

    @Override
    public Optional<AssessmentSessionSettings> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public AssessmentSessionSettings save(AssessmentSessionSettings s) {
        AssessmentSessionSettingsJpaEntity e = repo.findByTenantId(s.getTenantId())
                .orElseGet(AssessmentSessionSettingsJpaEntity::new);
        e.setTenantId(s.getTenantId());
        e.setQuizMinutes(s.getQuizMinutes());
        e.setHomeworkMinutes(s.getHomeworkMinutes());
        e.setExamMinutes(s.getExamMinutes());
        e.setFinalMinutes(s.getFinalMinutes());
        return toDomain(repo.save(e));
    }

    private AssessmentSessionSettings toDomain(AssessmentSessionSettingsJpaEntity e) {
        return AssessmentSessionSettings.builder()
                .id(e.getId()).tenantId(e.getTenantId())
                .quizMinutes(e.getQuizMinutes()).homeworkMinutes(e.getHomeworkMinutes())
                .examMinutes(e.getExamMinutes()).finalMinutes(e.getFinalMinutes())
                .build();
    }
}
