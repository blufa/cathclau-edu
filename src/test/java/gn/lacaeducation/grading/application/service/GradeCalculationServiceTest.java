package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeCalculationServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock GradeRepository gradeRepository;
    @Mock ReportCardRepository reportCardRepository;
    @Mock ClassSubjectQueryPort classSubjectQueryPort;
    @Mock GradingScaleRepository gradingScaleRepository;
    @Mock gn.lacaeducation.grading.application.port.out.EliminatoryRuleRepository eliminatoryRuleRepository;
    @Mock gn.lacaeducation.grading.application.port.out.DecisionRuleRepository decisionRuleRepository;
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
    void generateForStudent_secondaire_computesWeightedGeneralAverageFromSubjectSemesterAverages() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        // Deux notes « cours » le même mois (pondérées par coefficient d'évaluation), pas de composition.
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 5))
                .coefficient(BigDecimal.valueOf(2)).maxScore(BigDecimal.valueOf(20)).build();
        Assessment a2 = Assessment.builder().id(2L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 20))
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
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());

        // M_oct = (12×2 + 15×1)/3 = 13.00 ; aucune composition → M_cours seul = 13.00
        // weighted = 13 × 3 = 39.00 ; general = 39/3 = 13.00
        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getCoursAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getCompoAverage()).isNull();
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getWeightedAverage()).isEqualByComparingTo("39.00");

        ReportCard rc = rcCaptor.getAllValues().get(0);
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.00");
        assertThat(rc.getHonor()).isEqualTo("Passable");
        assertThat(rc.getDecision()).isEqualTo("ADMIS");
    }

    /**
     * Formule secondaire confirmée avec la direction (features/Calcul-moyenne.pdf,
     * Calcul-moyenne-2.pdf) : M_sem_matière = (M_cours + M_comp) / 2, M_cours = moyenne des moyennes
     * mensuelles. Vérifie l'intégration bout-en-bout (regroupement mensuel + composition + moyenne
     * générale pondérée par coefficient de matière) sur deux matières avec des coefficients différents.
     */
    @Test
    void generateForStudent_secondaire_monthlyCoursPlusCompositionFiftyFifty() {
        Long tenantId = 5L, classId = 1L, periodId = 2L, studentId = 10L;
        Long anglaisId = 100L, mathsId = 101L;

        List<Assessment> anglaisAssessments = List.of(
                Assessment.builder().id(1L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 10, 15)).build(),
                Assessment.builder().id(2L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 11, 15)).build(),
                Assessment.builder().id(3L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 12, 15)).build(),
                Assessment.builder().id(4L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(anglaisId)
                        .type(AssessmentType.EXAM).coefficient(BigDecimal.ONE).date(LocalDate.of(2026, 1, 10)).build());
        List<Assessment> mathsAssessments = List.of(
                Assessment.builder().id(5L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 10, 16)).build(),
                Assessment.builder().id(6L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 11, 16)).build(),
                Assessment.builder().id(7L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .type(AssessmentType.QUIZ).coefficient(BigDecimal.ONE).date(LocalDate.of(2025, 12, 16)).build(),
                Assessment.builder().id(8L).tenantId(tenantId).classId(classId).periodId(periodId).subjectId(mathsId)
                        .type(AssessmentType.EXAM).coefficient(BigDecimal.ONE).date(LocalDate.of(2026, 1, 11)).build());

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

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        java.util.Map<Long, ReportCardLine> lineBySubject = lineCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(ReportCardLine::getSubjectId, l -> l, (a, b) -> b));

        // Anglais : M_cours = (16.5+16.5+16.5)/3 = 16.50 ; M_comp = 12.00 ; M_sem = (16.5+12)/2 = 14.25
        // Points = 14.25 × 2 = 28.50
        ReportCardLine anglaisLine = lineBySubject.get(anglaisId);
        assertThat(anglaisLine.getCoursAverage()).isEqualByComparingTo("16.50");
        assertThat(anglaisLine.getCompoAverage()).isEqualByComparingTo("12.00");
        assertThat(anglaisLine.getSubjectAverage()).isEqualByComparingTo("14.25");
        assertThat(anglaisLine.getWeightedAverage()).isEqualByComparingTo("28.50");

        // Maths : M_cours = 15.00 ; M_comp = 12.00 ; M_sem = (15+12)/2 = 13.50 ; Points = 13.50 × 4 = 54.00
        ReportCardLine mathsLine = lineBySubject.get(mathsId);
        assertThat(mathsLine.getCoursAverage()).isEqualByComparingTo("15.00");
        assertThat(mathsLine.getCompoAverage()).isEqualByComparingTo("12.00");
        assertThat(mathsLine.getSubjectAverage()).isEqualByComparingTo("13.50");
        assertThat(mathsLine.getWeightedAverage()).isEqualByComparingTo("54.00");

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        // (28.50 + 54.00) / (2 + 4) = 82.50 / 6 = 13.75
        assertThat(rcCaptor.getAllValues().get(0).getOverallAverage()).isEqualByComparingTo("13.75");
    }

    @Test
    void generateForStudent_primaire_usesSimpleWeightedAverage_noCoursCompoSplit() {
        Long tenantId = 7L, classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        // Cycle primaire : aucun type/date requis, seule la note et le coefficient d'évaluation comptent.
        Assessment a1 = Assessment.builder().id(1L).tenantId(tenantId).classId(classId).periodId(periodId)
                .subjectId(subjectId).coefficient(BigDecimal.valueOf(1)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(1), 5L)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(13)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(classId, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));
        gn.lacaeducation.grading.domain.GradingScale primaryScale = gn.lacaeducation.grading.domain.GradingScale.builder()
                .tenantId(tenantId).cycleGroup(gn.lacaeducation.grading.domain.CycleGroup.PRIMAIRE)
                .passThreshold(BigDecimal.valueOf(5)).passableMin(BigDecimal.valueOf(5))
                .assezBienMin(BigDecimal.valueOf(8)).bienMin(BigDecimal.valueOf(11)).tresBienMin(BigDecimal.valueOf(12))
                .build();
        when(gradingScaleRepository.findByTenantIdAndCycleGroup(tenantId, gn.lacaeducation.grading.domain.CycleGroup.PRIMAIRE))
                .thenReturn(Optional.of(primaryScale));

        service.generateForStudent(studentId, classId, periodId);

        org.mockito.Mockito.verify(gradingScaleRepository)
                .findByTenantIdAndCycleGroup(tenantId, gn.lacaeducation.grading.domain.CycleGroup.PRIMAIRE);
        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getCoursAverage()).isNull();
        assertThat(line.getCompoAverage()).isNull();

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        // Avec le barème primaire (Très Bien dès 12), une moyenne de 13 doit être "Très Bien" — le
        // barème par défaut du secondaire (Très Bien dès 18) donnerait "Passable".
        assertThat(rcCaptor.getAllValues().get(rcCaptor.getAllValues().size() - 1).getHonor()).isEqualTo("Très Bien");
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
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 5)).coefficient(BigDecimal.ONE).build();
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
