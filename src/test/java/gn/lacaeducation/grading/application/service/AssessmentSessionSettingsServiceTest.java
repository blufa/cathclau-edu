package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.AssessmentSessionSettingsUseCase.UpdateSettingsCommand;
import gn.lacaeducation.grading.application.port.out.AssessmentSessionSettingsRepository;
import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import gn.lacaeducation.grading.domain.AssessmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentSessionSettingsServiceTest {

    @Mock AssessmentSessionSettingsRepository repo;
    @InjectMocks AssessmentSessionSettingsService service;

    @Test
    void sessionMinutesFor_noConfig_usesTypeDefaults() {
        when(repo.findByTenantId(1L)).thenReturn(Optional.empty());

        assertThat(service.sessionMinutesFor(1L, AssessmentType.EXAM))
                .isEqualTo(AssessmentType.EXAM.sessionMinutes()); // 120
    }

    @Test
    void sessionMinutesFor_tenantOverride_usesConfiguredValue() {
        when(repo.findByTenantId(1L)).thenReturn(Optional.of(AssessmentSessionSettings.builder()
                .tenantId(1L).quizMinutes(45).homeworkMinutes(90).examMinutes(150).finalMinutes(240).build()));

        assertThat(service.sessionMinutesFor(1L, AssessmentType.EXAM)).isEqualTo(150);
        assertThat(service.sessionMinutesFor(1L, AssessmentType.QUIZ)).isEqualTo(45);
    }

    @Test
    void update_outOfRange_throws() {
        assertThatThrownBy(() -> service.update(1L, new UpdateSettingsCommand(1, 60, 120, 180)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_nullValues_fallBackToDefaults() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        AssessmentSessionSettings saved = service.update(1L, new UpdateSettingsCommand(null, null, 150, null));

        assertThat(saved.getExamMinutes()).isEqualTo(150);
        assertThat(saved.getQuizMinutes()).isEqualTo(AssessmentType.QUIZ.sessionMinutes());
        assertThat(saved.getFinalMinutes()).isEqualTo(AssessmentType.FINAL.sessionMinutes());
    }
}
