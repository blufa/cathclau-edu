package gn.lacaeducation.lessons.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.lessons.application.event.LessonValidatedWithoutCheckinEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository repo;
    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academics;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks LessonService service;

    @Test
    void find_filtersByTeacherIdOnTopOfTenant() {
        LessonEntry forTeacher1 = LessonEntry.builder().id(1L).tenantId(1L).classId(10L).teacherId(100L).build();
        LessonEntry forTeacher2 = LessonEntry.builder().id(2L).tenantId(1L).classId(10L).teacherId(200L).build();
        when(repo.findByTenantId(1L)).thenReturn(List.of(forTeacher1, forTeacher2));

        List<LessonEntry> result = service.find(1L, null, null, 100L);

        assertThat(result).containsExactly(forTeacher1);
    }

    @Test
    void find_filtersByTeacherIdAndClassId() {
        LessonEntry match = LessonEntry.builder().id(1L).tenantId(1L).classId(10L).teacherId(100L).build();
        LessonEntry wrongTeacher = LessonEntry.builder().id(2L).tenantId(1L).classId(10L).teacherId(200L).build();
        when(repo.findByTenantIdAndClassId(1L, 10L)).thenReturn(List.of(match, wrongTeacher));

        List<LessonEntry> result = service.find(1L, 10L, null, 100L);

        assertThat(result).containsExactly(match);
    }

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

    @Test
    void create_resolvesTeacherFromClassSubjectAssignment() {
        when(academics.findClassSubjects(5L)).thenReturn(List.of(
                new ClassSubjectPublicInfo(5L, 7L, BigDecimal.ONE, 3L, "MA1")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonEntry result = service.create(new LessonService.CreateLessonCommand(
                1L, 5L, 7L, null, LocalDate.now(), "Fractions", null, null, 60, null));

        assertThat(result.getTeacherId()).isEqualTo(3L);
    }

    @Test
    void create_throwsWhenNoTeacherAssignedForSubject() {
        when(academics.findClassSubjects(5L)).thenReturn(List.of(
                new ClassSubjectPublicInfo(5L, 7L, BigDecimal.ONE, null, null)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(new LessonService.CreateLessonCommand(
                        1L, 5L, 7L, null, LocalDate.now(), "Fractions", null, null, 60, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Aucun enseignant");
        verify(repo, never()).save(any());
    }

    @Test
    void create_withoutSubject_resolvesTeacherFromClassHeadTeacher() {
        // Moniteur maternelle/primaire : un seul enseignant couvre toute la classe, salaire fixe —
        // pas de matière à choisir, l'enseignant se résout via le professeur principal de la classe.
        when(academics.findClass(5L)).thenReturn(java.util.Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(5L, "Maternelle A", "Maternelle", "NORMALE", null, 8L, null)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonEntry result = service.create(new LessonService.CreateLessonCommand(
                1L, 5L, null, null, LocalDate.now(), "Journée du 26/08", null, null, 300, null));

        assertThat(result.getTeacherId()).isEqualTo(8L);
        assertThat(result.getSubjectId()).isNull();
        verify(academics, never()).findClassSubjects(any());
    }

    @Test
    void create_withoutSubject_throwsWhenNoHeadTeacherDesignated() {
        when(academics.findClass(5L)).thenReturn(java.util.Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(5L, "Maternelle A", "Maternelle", "NORMALE", null, null, null)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(new LessonService.CreateLessonCommand(
                        1L, 5L, null, null, LocalDate.now(), "Journée du 26/08", null, null, 300, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("professeur principal");
        verify(repo, never()).save(any());
    }

    // ── markDone (§retour fondateurs : trace d'audit quand validée sans pointage enseignant) ───────

    @Test
    void markDone_withoutConfirmedWithoutCheckin_publishesNoEvent() {
        LessonEntry existing = LessonEntry.builder().id(10L).tenantId(1L).teacherId(3L)
                .lessonDate(LocalDate.now()).status(LessonStatus.PLANNED).build();
        when(repo.findById(10L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonEntry result = service.markDone(10L, 45);

        assertThat(result.getStatus()).isEqualTo(LessonStatus.DONE);
        assertThat(result.getDurationMinutes()).isEqualTo(45);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void markDone_confirmedWithoutCheckin_publishesAuditEvent() {
        LessonEntry existing = LessonEntry.builder().id(10L).tenantId(1L).teacherId(3L)
                .lessonDate(LocalDate.of(2026, 10, 1)).status(LessonStatus.PLANNED).build();
        when(repo.findById(10L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.markDone(10L, null, true);

        ArgumentCaptor<LessonValidatedWithoutCheckinEvent> captor =
                ArgumentCaptor.forClass(LessonValidatedWithoutCheckinEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(1L);
        assertThat(captor.getValue().lessonId()).isEqualTo(10L);
        assertThat(captor.getValue().teacherId()).isEqualTo(3L);
        assertThat(captor.getValue().lessonDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void update_reResolvesTeacherFromClassSubjectAssignment() {
        LessonEntry existing = LessonEntry.builder().id(10L).tenantId(1L).classId(5L).subjectId(7L).teacherId(99L).build();
        when(repo.findById(10L)).thenReturn(java.util.Optional.of(existing));
        when(academics.findClassSubjects(5L)).thenReturn(List.of(
                new ClassSubjectPublicInfo(5L, 7L, BigDecimal.ONE, 3L, "MA1")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        LessonEntry result = service.update(10L, new LessonService.CreateLessonCommand(
                1L, 5L, 7L, 99L, LocalDate.now(), "Fractions", null, null, 60, null));

        // Le professeur envoyé par le client (99L, obsolète) est ignoré au profit de l'affectation réelle.
        assertThat(result.getTeacherId()).isEqualTo(3L);
    }
}
