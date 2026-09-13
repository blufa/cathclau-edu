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
import gn.lacaeducation.grading.domain.GradingFormulaStepInput;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.StepKind;
import gn.lacaeducation.grading.domain.StepRole;
import gn.lacaeducation.grading.domain.WeightMode;
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
import static org.mockito.ArgumentMatchers.eq;
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

    /**
     * Valide les règles mathématiques rigoureuses du système guinéen (formule « mensuel + composition »,
     * exactement le modèle proposé par le bouton « Modèle mensuel + composition » du panneau de
     * configuration) sur les deux cas chiffrés fournis : Anglais coef. 2 (Moy_Cours 16.5, Composition
     * 12 → Moy_Matiere_Brute 13.50 → Points 27.00) et Mathématiques coef. 4 (Moy_Cours 15.0, Composition
     * 12 → Moy_Matiere_Brute 13.00 → Points 52.00), puis la moyenne générale attendue :
     * (27.00 + 52.00) / (2 + 4) = 13.17 — <b>pas</b> 13.25, qui était le résultat de l'ancien calcul
     * (moyenne de cours simple entre matières + moyenne de composition pondérée, combinées séparément),
     * retiré car il ne correspond pas à la règle confirmée {@code Σ(Points_Matiere) / Σ(Coefficients)}.
     */
    @Test
    void generateForStudent_guineanMonthlyCompositionFormula_matchesOfficialWorkedExamples() {
        Long tenantId = 5L, classId = 1L, periodId = 2L, studentId = 10L;
        Long anglaisId = 100L, mathsId = 101L, compositionTypeId = 77L;

        List<Assessment> anglaisAssessments = List.of(
                Assessment.builder().id(1L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 10, 15)).build(),
                Assessment.builder().id(2L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 11, 15)).build(),
                Assessment.builder().id(3L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 12, 15)).build(),
                Assessment.builder().id(4L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .assessmentTypeId(compositionTypeId).coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20))
                        .date(LocalDate.of(2026, 1, 10)).build());
        List<Assessment> mathsAssessments = List.of(
                Assessment.builder().id(5L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 10, 16)).build(),
                Assessment.builder().id(6L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 11, 16)).build(),
                Assessment.builder().id(7L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).date(LocalDate.of(2025, 12, 16)).build(),
                Assessment.builder().id(8L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .assessmentTypeId(compositionTypeId).coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20))
                        .date(LocalDate.of(2026, 1, 11)).build());

        List<Assessment> all = new java.util.ArrayList<>();
        all.addAll(anglaisAssessments);
        all.addAll(mathsAssessments);
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(all);
        when(classSubjectQueryPort.findByClassId(classId)).thenReturn(List.of(
                new ClassSubjectInfo(anglaisId, BigDecimal.valueOf(2), 50L),
                new ClassSubjectInfo(mathsId, BigDecimal.valueOf(4), 51L)));

        BigDecimal[] anglaisGrades = { BigDecimal.valueOf(16.5), BigDecimal.valueOf(16.5), BigDecimal.valueOf(16.5), BigDecimal.valueOf(12) };
        for (int i = 0; i < anglaisAssessments.size(); i++) {
            long aid = anglaisAssessments.get(i).getId();
            when(gradeRepository.findByAssessmentId(aid)).thenReturn(
                    List.of(Grade.builder().assessmentId(aid).studentId(studentId).value(anglaisGrades[i]).build()));
        }
        BigDecimal[] mathsGrades = { BigDecimal.valueOf(15), BigDecimal.valueOf(15), BigDecimal.valueOf(15), BigDecimal.valueOf(12) };
        for (int i = 0; i < mathsAssessments.size(); i++) {
            long aid = mathsAssessments.get(i).getId();
            when(gradeRepository.findByAssessmentId(aid)).thenReturn(
                    List.of(Grade.builder().assessmentId(aid).studentId(studentId).value(mathsGrades[i]).build()));
        }

        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        when(academicsDataProvider.findPeriod(periodId)).thenReturn(Optional.of(
                new PeriodPublicInfo(periodId, "Semestre 1", "SEMESTRE", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 1, 31))));

        GradingFormulaStep month1 = GradingFormulaStep.builder().stepOrder(1).stepKind(StepKind.GROUP).monthIndex(1).label("Mois 1").build();
        GradingFormulaStep month2 = GradingFormulaStep.builder().stepOrder(2).stepKind(StepKind.GROUP).monthIndex(2).label("Mois 2").build();
        GradingFormulaStep month3 = GradingFormulaStep.builder().stepOrder(3).stepKind(StepKind.GROUP).monthIndex(3).label("Mois 3").build();
        GradingFormulaStep cours = GradingFormulaStep.builder().stepOrder(4).stepKind(StepKind.COMBINE).role(StepRole.COURS)
                .label("Moyenne de cours")
                .inputs(List.of(
                        GradingFormulaStepInput.builder().refStepOrder(1).weightMode(WeightMode.EQUAL).build(),
                        GradingFormulaStepInput.builder().refStepOrder(2).weightMode(WeightMode.EQUAL).build(),
                        GradingFormulaStepInput.builder().refStepOrder(3).weightMode(WeightMode.EQUAL).build()))
                .build();
        GradingFormulaStep composition = GradingFormulaStep.builder().stepOrder(5).stepKind(StepKind.GROUP)
                .assessmentTypeId(compositionTypeId).role(StepRole.COMPOSITION).label("Composition").build();
        GradingFormulaStep brute = GradingFormulaStep.builder().stepOrder(6).stepKind(StepKind.COMBINE).finalStep(true)
                .label("Moyenne de matière")
                .inputs(List.of(
                        GradingFormulaStepInput.builder().refStepOrder(4).weightMode(WeightMode.EQUAL).build(),
                        GradingFormulaStepInput.builder().refStepOrder(5).weightMode(WeightMode.FIXED).fixedWeight(BigDecimal.valueOf(2)).build()))
                .build();
        GradingFormula formula = GradingFormula.builder().id(1L).tenantId(tenantId).label("Mensuel + composition")
                .periodType("SEMESTRE").active(true)
                .steps(List.of(month1, month2, month3, cours, composition, brute))
                .build();
        when(gradingFormulaRepository.findActiveByTenantIdAndPeriodType(tenantId, "SEMESTRE")).thenReturn(Optional.of(formula));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        java.util.Map<Long, ReportCardLine> lineBySubject = lineCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(ReportCardLine::getSubjectId, l -> l, (a, b) -> b));

        ReportCardLine anglaisLine = lineBySubject.get(anglaisId);
        assertThat(anglaisLine.getCoursAverage()).isEqualByComparingTo("16.5");
        assertThat(anglaisLine.getSubjectAverage()).isEqualByComparingTo("13.50");
        assertThat(anglaisLine.getWeightedAverage()).isEqualByComparingTo("27.00");

        ReportCardLine mathsLine = lineBySubject.get(mathsId);
        assertThat(mathsLine.getCoursAverage()).isEqualByComparingTo("15.0");
        assertThat(mathsLine.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(mathsLine.getWeightedAverage()).isEqualByComparingTo("52.00");

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        // (27.00 + 52.00) / (2 + 4) = 13.1666... arrondi à 13.17 — jamais 13.25 (ancien calcul retiré).
        assertThat(rcCaptor.getAllValues().get(0).getOverallAverage()).isEqualByComparingTo("13.17");
    }

    /**
     * Ex æquo (règle confirmée §4) : deux élèves à moyenne strictement identique partagent le même
     * rang, et l'élève suivant reprend au rang correspondant à sa position réelle (1, 1, 3 — jamais
     * 1, 2, 3) — vrai pour le rang général ET le rang par matière.
     */
    @Test
    void generateForClass_tiedAverages_shareRankAndSkipNextPosition() {
        Long classId = 1L, periodId = 2L, subjectId = 100L;
        Long studentA = 10L, studentB = 11L, studentC = 12L;
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, 5L)));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(studentA).value(BigDecimal.valueOf(15)).build(),
                Grade.builder().assessmentId(1L).studentId(studentB).value(BigDecimal.valueOf(15)).build(),
                Grade.builder().assessmentId(1L).studentId(studentC).value(BigDecimal.valueOf(10)).build()));

        when(reportCardRepository.findByStudentIdAndPeriodId(any(), eq(periodId))).thenReturn(Optional.empty());
        java.util.Map<Long, ReportCard> savedByStudent = new java.util.HashMap<>();
        when(reportCardRepository.save(any())).thenAnswer(i -> {
            ReportCard r = i.getArgument(0);
            if (r.getId() == null) r.setId(savedByStudent.size() + 1L);
            savedByStudent.put(r.getStudentId(), r);
            return r;
        });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findLines(any())).thenReturn(List.of());

        List<ReportCard> generated = service.generateForClass(classId, periodId);

        java.util.Map<Long, Integer> rankByStudent = generated.stream()
                .collect(java.util.stream.Collectors.toMap(ReportCard::getStudentId, ReportCard::getRank));
        assertThat(rankByStudent.get(studentA)).isEqualTo(1);
        assertThat(rankByStudent.get(studentB)).isEqualTo(1);
        assertThat(rankByStudent.get(studentC)).isEqualTo(3);
    }
}
