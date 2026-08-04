package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.PeriodPublicInfo;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.GradingFormulaRepository;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.GradingFormula;
import gn.lacaeducation.grading.domain.GradingFormulaStep;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.StepKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeCalculationServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock ReportCardRepository reportCardRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock GradingScaleRepository gradingScaleRepository;
    @Mock gn.lacaeducation.grading.application.port.out.GradingCalculationSettingsRepository calculationSettingsRepository;
    @Mock gn.lacaeducation.grading.application.port.out.EliminatoryRuleRepository eliminatoryRuleRepository;
    @Mock gn.lacaeducation.grading.application.port.out.DecisionRuleRepository decisionRuleRepository;
    @Mock gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository assessmentTypeConfigRepository;
    @Mock GradingFormulaRepository gradingFormulaRepository;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks GradeCalculationService service;

    @Test
    void honorAndDecision_thresholds() {
        gn.lacaeducation.grading.domain.GradingScale scale = gn.lacaeducation.grading.domain.GradingScale.defaults();
        assertThat(scale.honorFor(BigDecimal.valueOf(18))).isEqualTo("Très Bien");
        assertThat(scale.honorFor(BigDecimal.valueOf(16))).isEqualTo("Bien");
        assertThat(scale.honorFor(BigDecimal.valueOf(14))).isEqualTo("Assez Bien");
        assertThat(scale.honorFor(BigDecimal.valueOf(10))).isEqualTo("Passable");
        assertThat(scale.honorFor(BigDecimal.valueOf(8))).isEqualTo("Insuffisant");
        assertThat(scale.decisionFor(BigDecimal.valueOf(10))).isEqualTo("ADMIS");
        assertThat(scale.decisionFor(BigDecimal.valueOf(9))).isEqualTo("REDOUBLE");
    }

    @Test
    void generateForStudent_computesWeightedAverages() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(2)).maxScore(BigDecimal.valueOf(20)).build();
        Assessment a2 = Assessment.builder().id(2L).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(1)).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1, a2));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(3), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(2L))
                .thenReturn(List.of(Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(15)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, org.mockito.Mockito.atLeastOnce()).save(rcCaptor.capture());
        org.mockito.Mockito.verify(reportCardRepository, org.mockito.Mockito.atLeastOnce()).saveLine(lineCaptor.capture());

        // subject average = (12*2 + 15*1) / 3 = 13.00 ; weighted = 13 * 3 = 39.00 ; general = 39/3 = 13.00
        ReportCard rc = rcCaptor.getAllValues().get(0);
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.00");
        assertThat(rc.getHonor()).isEqualTo("Passable");
        assertThat(rc.getDecision()).isEqualTo("ADMIS");

        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getWeightedAverage()).isEqualByComparingTo("39.00");
    }

    @Test
    void generateForStudent_noFormulaConfigured_autoCreatesLegacyEquivalentFormula() {
        // Preuve de non-régression (§ migration) : un tenant sans formule obtient automatiquement une
        // formule « Héritée » à une seule étape, et le résultat reste identique à l'ancien calcul plat.
        Long tenantId = 5L, classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        Assessment a1 = Assessment.builder().id(1L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(2)).maxScore(BigDecimal.valueOf(20)).build();
        Assessment a2 = Assessment.builder().id(2L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(1)).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1, a2));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(3), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(2L))
                .thenReturn(List.of(Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(15)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        when(academicsDataProvider.findPeriod(periodId)).thenReturn(Optional.of(
                new PeriodPublicInfo(periodId, "Semestre 1", "SEMESTRE", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 1, 31))));
        when(gradingFormulaRepository.findActiveByTenantIdAndPeriodType(tenantId, "SEMESTRE")).thenReturn(Optional.empty());
        when(gradingFormulaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        // Même résultat que l'ancien calcul plat pondéré : (12×2 + 15×1) / 3 = 13.00
        assertThat(rcCaptor.getAllValues().get(0).getOverallAverage()).isEqualByComparingTo("13.00");

        ArgumentCaptor<GradingFormula> formulaCaptor = ArgumentCaptor.forClass(GradingFormula.class);
        verify(gradingFormulaRepository).save(formulaCaptor.capture());
        GradingFormula created = formulaCaptor.getValue();
        assertThat(created.getTenantId()).isEqualTo(tenantId);
        assertThat(created.getPeriodType()).isEqualTo("SEMESTRE");
        assertThat(created.isActive()).isTrue();
        assertThat(created.getSteps()).hasSize(1);
        assertThat(created.getSteps().get(0).isFinalStep()).isTrue();
    }

    @Test
    void generateForStudent_withConfiguredFormula_usesEngineInsteadOfLegacyFlatAverage() {
        // Une formule configurée doit réellement piloter le calcul, pas juste exister sans effet :
        // un filtre qui ne correspond à aucune évaluation donne 0.00, très différent du 12.00 brut.
        Long tenantId = 5L, classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        Assessment a1 = Assessment.builder().id(1L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.valueOf(2)).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(3), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        when(academicsDataProvider.findPeriod(periodId)).thenReturn(Optional.of(
                new PeriodPublicInfo(periodId, "Semestre 1", "SEMESTRE", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 1, 31))));
        GradingFormulaStep noMatchGroup = GradingFormulaStep.builder()
                .stepOrder(1).stepKind(StepKind.GROUP).assessmentTypeId(77L).finalStep(true).build();
        GradingFormula configured = GradingFormula.builder()
                .id(9L).tenantId(tenantId).label("Custom").periodType("SEMESTRE").active(true)
                .steps(List.of(noMatchGroup)).build();
        when(gradingFormulaRepository.findActiveByTenantIdAndPeriodType(tenantId, "SEMESTRE")).thenReturn(Optional.of(configured));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        assertThat(lineCaptor.getAllValues().get(0).getSubjectAverage()).isEqualByComparingTo("0.00");

        verify(gradingFormulaRepository, never()).save(any());
    }
}
