package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase.CreateAssessmentCommand;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase.DuplicateGroup;
import gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock AssessmentRepository repo;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock StudentDataProvider studentDataProvider;
    @Mock GradeRepository gradeRepository;
    @Mock gn.lacaeducation.grading.application.port.out.AssessmentPlanPdfGenerator planPdfGenerator;
    @Mock AssessmentGradeSheetPdfGenerator gradeSheetPdfGenerator;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks AssessmentService service;

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    private CreateAssessmentCommand cmd() {
        return new CreateAssessmentCommand(10L, 1L, 100L, 5L, 2L, AssessmentType.HOMEWORK, null,
                "Devoir 1", LocalDate.of(2026, 1, 15), BigDecimal.ONE, BigDecimal.valueOf(20));
    }

    @Test
    void create_closedPeriod_throws() {
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd())).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_openPeriod_saves() {
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Assessment a = service.create(cmd());

        org.assertj.core.api.Assertions.assertThat(a.getLabel()).isEqualTo("Devoir 1");
    }

    @Test
    void create_duplicateClassSubjectPeriodTypeLabelDate_throws() {
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.existsDuplicate(1L, 100L, 2L, AssessmentType.HOMEWORK, "Devoir 1",
                LocalDate.of(2026, 1, 15), null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd())).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_excludesItselfFromDuplicateCheck() {
        Assessment existing = Assessment.builder().id(9L).periodId(2L).build();
        when(repo.findById(9L)).thenReturn(Optional.of(existing));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Assessment a = service.update(9L, cmd());

        assertThat(a.getLabel()).isEqualTo("Devoir 1");
        verify(repo).existsDuplicate(1L, 100L, 2L, AssessmentType.HOMEWORK, "Devoir 1",
                LocalDate.of(2026, 1, 15), 9L);
    }

    @Test
    void assessmentGradeSheetPdf_includesAlreadyEnteredGrades() {
        Assessment a = Assessment.builder().id(9L).tenantId(10L).classId(1L).subjectId(100L)
                .type(AssessmentType.HOMEWORK).label("Anglais — Oral — Octobre")
                .date(LocalDate.of(2026, 10, 15)).maxScore(BigDecimal.valueOf(20)).build();
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        when(academicsDataProvider.findClass(1L)).thenReturn(Optional.of(
                new ClassPublicInfo(1L, "6eme A", "6eme", "NORMALE", null, null, "COLLEGE")));
        when(academicsDataProvider.subjectLabels(java.util.Set.of(100L))).thenReturn(java.util.Map.of(100L, "Anglais"));
        StudentPublicInfo s1 = new StudentPublicInfo(1L, 10L, "Aissatou", "Diallo", null, null, null, null, null, null, null);
        StudentPublicInfo s2 = new StudentPublicInfo(2L, 10L, "Mamadou", "Bah", null, null, null, null, null, null, null);
        when(studentDataProvider.findStudentsByClass(1L)).thenReturn(List.of(s1, s2));
        when(gradeRepository.findByAssessmentId(9L)).thenReturn(List.of(
                Grade.builder().assessmentId(9L).studentId(1L).value(BigDecimal.valueOf(16.5)).build()));
        when(gradeSheetPdfGenerator.generate(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = service.assessmentGradeSheetPdf(9L);

        assertThat(pdf).isEqualTo(new byte[]{1, 2, 3});
        ArgumentCaptor<AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet> captor =
                ArgumentCaptor.forClass(AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet.class);
        verify(gradeSheetPdfGenerator).generate(captor.capture());
        AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet sheet = captor.getValue();
        assertThat(sheet.subjectName()).isEqualTo("Anglais");
        assertThat(sheet.rows()).hasSize(2);
        // Bah (sans note) trié avant Diallo par nom de famille.
        assertThat(sheet.rows().get(0).studentName()).isEqualTo("Mamadou Bah");
        assertThat(sheet.rows().get(0).value()).isNull();
        assertThat(sheet.rows().get(1).studentName()).isEqualTo("Aissatou Diallo");
        assertThat(sheet.rows().get(1).value()).isEqualByComparingTo("16.5");
    }

    @Test
    void delete_cascadesGradesBeforeAssessment() {
        Assessment a = Assessment.builder().id(9L).periodId(2L).build();
        when(repo.findById(9L)).thenReturn(Optional.of(a));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);

        service.delete(9L);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(gradeRepository, repo);
        order.verify(gradeRepository).deleteByAssessmentId(9L);
        order.verify(repo).deleteById(9L);
    }

    @Test
    void findDuplicateQuizGroups_groupsQuizAssessmentsSharingClassSubjectPeriodDate() {
        TenantContext.set(10L);
        Assessment orale = Assessment.builder().id(1L).tenantId(10L).classId(1L).subjectId(100L).periodId(2L)
                .type(AssessmentType.QUIZ).label("Oral").date(LocalDate.of(2026, 10, 10)).build();
        Assessment ecrite = Assessment.builder().id(2L).tenantId(10L).classId(1L).subjectId(100L).periodId(2L)
                .type(AssessmentType.QUIZ).label("Ecrit").date(LocalDate.of(2026, 10, 10)).build();
        // Un devoir (HOMEWORK) même jour — jamais considéré comme un doublon d'interrogation.
        Assessment homework = Assessment.builder().id(3L).tenantId(10L).classId(1L).subjectId(100L).periodId(2L)
                .type(AssessmentType.HOMEWORK).label("Devoir").date(LocalDate.of(2026, 10, 10)).build();
        // Une interrogation isolée (aucun doublon) — ne doit jamais apparaître dans le résultat.
        Assessment alone = Assessment.builder().id(4L).tenantId(10L).classId(5L).subjectId(200L).periodId(2L)
                .type(AssessmentType.QUIZ).label("Interro").date(LocalDate.of(2026, 10, 11)).build();
        when(repo.findByTenantId(10L)).thenReturn(List.of(orale, ecrite, homework, alone));

        List<DuplicateGroup> groups = service.findDuplicateQuizGroups();

        assertThat(groups).hasSize(1);
        DuplicateGroup group = groups.get(0);
        assertThat(group.classId()).isEqualTo(1L);
        assertThat(group.subjectId()).isEqualTo(100L);
        assertThat(group.assessments()).extracting(Assessment::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void mergeDuplicates_combinesGradesIntoValueAAndValueB_thenDeletesTheOther() {
        Assessment keep = Assessment.builder().id(1L).classId(1L).subjectId(100L).periodId(2L).build();
        Assessment remove = Assessment.builder().id(2L).classId(1L).subjectId(100L).periodId(2L).build();
        when(repo.findById(1L)).thenReturn(Optional.of(keep));
        when(repo.findById(2L)).thenReturn(Optional.of(remove));
        when(academicsDataProvider.isPeriodOpenForGrades(2L)).thenReturn(true);
        // Élève 10 : noté sur les deux évaluations → fusion en valueA/valueB.
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().id(100L).assessmentId(1L).studentId(10L).value(BigDecimal.valueOf(14)).build()));
        // Élève 20 : noté UNIQUEMENT sur l'évaluation supprimée → sa note ne doit pas disparaître.
        when(gradeRepository.findByAssessmentId(2L)).thenReturn(List.of(
                Grade.builder().id(101L).assessmentId(2L).studentId(10L).value(BigDecimal.valueOf(18)).build(),
                Grade.builder().id(102L).assessmentId(2L).studentId(20L).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.mergeDuplicates(1L, 2L);

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Grade mergedStudent10 = captor.getAllValues().stream().filter(g -> g.getStudentId().equals(10L)).findFirst().orElseThrow();
        assertThat(mergedStudent10.getValueA()).isEqualByComparingTo("14");
        assertThat(mergedStudent10.getValueB()).isEqualByComparingTo("18");
        assertThat(mergedStudent10.getValue()).isEqualByComparingTo("16.00");
        Grade carriedStudent20 = captor.getAllValues().stream().filter(g -> g.getStudentId().equals(20L)).findFirst().orElseThrow();
        assertThat(carriedStudent20.getAssessmentId()).isEqualTo(1L);
        assertThat(carriedStudent20.getValueB()).isEqualByComparingTo("12");
        assertThat(carriedStudent20.getValue()).isEqualByComparingTo("12");

        verify(gradeRepository).deleteByAssessmentId(2L);
        verify(repo).deleteById(2L);
    }

    @Test
    void mergeDuplicates_differentClassOrSubject_throws() {
        Assessment keep = Assessment.builder().id(1L).classId(1L).subjectId(100L).periodId(2L).build();
        Assessment remove = Assessment.builder().id(2L).classId(99L).subjectId(100L).periodId(2L).build();
        when(repo.findById(1L)).thenReturn(Optional.of(keep));
        when(repo.findById(2L)).thenReturn(Optional.of(remove));

        assertThatThrownBy(() -> service.mergeDuplicates(1L, 2L)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).deleteById(any());
    }

    @Test
    void mergeDuplicates_sameId_throws() {
        assertThatThrownBy(() -> service.mergeDuplicates(1L, 1L)).isInstanceOf(BusinessRuleException.class);
    }
}
