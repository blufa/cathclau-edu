package gn.lacaeducation.lessons.application.service;

import gn.lacaeducation.lessons.application.port.out.LessonRepository;
import gn.lacaeducation.lessons.domain.LessonEntry;
import gn.lacaeducation.lessons.domain.LessonStatus;
import gn.lacaeducation.students.StudentDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository repo;
    @Mock StudentDataProvider studentDataProvider;
    @InjectMocks LessonService service;

    @Test
    void recordAssessmentSession_pastDate_createsDoneSessionWithTypeDuration() {
        when(repo.existsByTenantIdAndSourceAssessmentId(1L, 99L)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        // 120 min = durée d'une composition (EXAM)
        service.recordAssessmentSession(1L, 5L, 7L, 3L, LocalDate.now().minusDays(1), "Composition 1", 99L, 120);

        ArgumentCaptor<LessonEntry> captor = ArgumentCaptor.forClass(LessonEntry.class);
        verify(repo).save(captor.capture());
        LessonEntry saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(LessonStatus.DONE);
        assertThat(saved.getTeacherId()).isEqualTo(3L);
        assertThat(saved.getSourceAssessmentId()).isEqualTo(99L);
        assertThat(saved.getDurationMinutes()).isEqualTo(120);
        assertThat(saved.getTitle()).contains("Composition 1");
    }

    @Test
    void recordAssessmentSession_noSessionMinutes_fallsBackToDefault() {
        when(repo.existsByTenantIdAndSourceAssessmentId(1L, 99L)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordAssessmentSession(1L, 5L, 7L, 3L, LocalDate.now(), "Sans type", 99L, null);

        ArgumentCaptor<LessonEntry> captor = ArgumentCaptor.forClass(LessonEntry.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getDurationMinutes())
                .isEqualTo(LessonService.DEFAULT_ASSESSMENT_SESSION_MINUTES);
    }

    @Test
    void recordAssessmentSession_futureDate_createsPlannedSession() {
        when(repo.existsByTenantIdAndSourceAssessmentId(1L, 99L)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.recordAssessmentSession(1L, 5L, 7L, 3L, LocalDate.now().plusDays(3), "Devoir", 99L, 60);

        ArgumentCaptor<LessonEntry> captor = ArgumentCaptor.forClass(LessonEntry.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LessonStatus.PLANNED);
    }

    @Test
    void recordAssessmentSession_alreadyRecorded_isIdempotent() {
        when(repo.existsByTenantIdAndSourceAssessmentId(1L, 99L)).thenReturn(true);

        LessonEntry result = service.recordAssessmentSession(1L, 5L, 7L, 3L, LocalDate.now(), "X", 99L, 30);

        assertThat(result).isNull();
        verify(repo, never()).save(any());
    }
}
