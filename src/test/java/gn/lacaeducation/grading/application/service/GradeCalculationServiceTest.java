package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
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
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock TeacherNameProvider teacherNameProvider;
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
        // Deux notes « cours » le même mois. Le coefficient qui compte est celui de la matière
        // (ClassSubjectInfo, confirmé avec l'utilisateur) — plus celui de chaque évaluation, qui n'est
        // plus lu par le moteur de calcul (voir SubjectSemesterAverage).
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 5))
                .maxScore(BigDecimal.valueOf(20)).build();
        Assessment a2 = Assessment.builder().id(2L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 20))
                .maxScore(BigDecimal.valueOf(20)).build();
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

        // M_oct = coef_matière × (12+15)/2 = 3 × 13.5 = 40.50 ; aucune composition → M_cours seul
        // Points_matière = 40.50 (déjà coefficiée) ; moyenne générale = 40.50 / 3 = 13.50
        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getCoursAverage()).isEqualByComparingTo("40.50");
        assertThat(line.getCompoAverage()).isNull();
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("40.50");
        assertThat(line.getWeightedAverage()).isEqualByComparingTo("40.50");

        ReportCard rc = rcCaptor.getAllValues().get(0);
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.50");
        assertThat(rc.getHonor()).isEqualTo("Passable");
        assertThat(rc.getDecision()).isEqualTo("ADMIS");
    }

    /**
     * Régression (testé en direct, école "Notre Mère") : le bulletin affichait littéralement
     * "teacher-30" (placeholder jamais résolu) au lieu du nom de l'enseignant.
     */
    @Test
    void generateForStudent_resolvesRealTeacherName_notPlaceholder() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L, teacherId = 5L;
        Assessment a1 = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.EXAM).date(LocalDate.of(2026, 10, 5))
                .coefficient(BigDecimal.ONE).maxScore(BigDecimal.valueOf(20)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(a1));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, teacherId)));
        when(gradeRepository.findByAssessmentId(1L))
                .thenReturn(List.of(Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(14)).build()));
        when(teacherNameProvider.teacherName(teacherId)).thenReturn(Optional.of("Lamine Fodé SYLLA"));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        assertThat(lineCaptor.getAllValues().get(0).getTeacherName()).isEqualTo("Lamine Fodé SYLLA");
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

        // Anglais (coef matière 2) : chaque mois n'a qu'une note -> M_mois = 2×16.5 = 33.00 (déjà
        // coefciée, pas normalisée) ; M_cours = 33.00 ; M_comp = 2×12 = 24.00 ; M_sem = (33+24)/2 = 28.50
        ReportCardLine anglaisLine = lineBySubject.get(anglaisId);
        assertThat(anglaisLine.getCoursAverage()).isEqualByComparingTo("33.00");
        assertThat(anglaisLine.getCompoAverage()).isEqualByComparingTo("24.00");
        assertThat(anglaisLine.getSubjectAverage()).isEqualByComparingTo("28.50");
        assertThat(anglaisLine.getWeightedAverage()).isEqualByComparingTo("28.50");

        // Maths (coef matière 4) : M_mois = 4×15 = 60.00 ; M_cours = 60.00 ; M_comp = 4×12 = 48.00 ;
        // M_sem = (60+48)/2 = 54.00
        ReportCardLine mathsLine = lineBySubject.get(mathsId);
        assertThat(mathsLine.getCoursAverage()).isEqualByComparingTo("60.00");
        assertThat(mathsLine.getCompoAverage()).isEqualByComparingTo("48.00");
        assertThat(mathsLine.getSubjectAverage()).isEqualByComparingTo("54.00");
        assertThat(mathsLine.getWeightedAverage()).isEqualByComparingTo("54.00");

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        // (28.50 + 54.00) / (2 + 4) = 82.50 / 6 = 13.75
        assertThat(rcCaptor.getAllValues().get(0).getOverallAverage()).isEqualByComparingTo("13.75");
    }

    /**
     * Primaire (§retour pédagogique) : seules les compositions (EXAM) comptent pour la moyenne de
     * matière, en moyenne simple — jamais pondérée par le coefficient de l'évaluation, contrairement
     * au Secondaire. Le barème est désormais toujours le standard par défaut (plus de surcharge par
     * tenant) : avec une moyenne de 13, l'honneur attendu est "Passable" (assezBienMin=14).
     */
    @Test
    void generateForStudent_primaire_usesSimpleAverageOfCompositionsOnly() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        // Une interrogation (QUIZ, ignorée en primaire) et deux compositions (EXAM, seules comptées).
        Assessment quiz = Assessment.builder().id(1L).classId(classId).periodId(periodId)
                .subjectId(subjectId).type(AssessmentType.QUIZ).coefficient(BigDecimal.valueOf(5)).build();
        Assessment exam1 = Assessment.builder().id(2L).classId(classId).periodId(periodId)
                .subjectId(subjectId).type(AssessmentType.EXAM).coefficient(BigDecimal.valueOf(3)).build();
        Assessment exam2 = Assessment.builder().id(3L).classId(classId).periodId(periodId)
                .subjectId(subjectId).type(AssessmentType.EXAM).coefficient(BigDecimal.valueOf(1)).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(quiz, exam1, exam2));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(1), 5L)));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(20)).build()));
        when(gradeRepository.findByAssessmentId(2L)).thenReturn(List.of(
                Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(3L)).thenReturn(List.of(
                Grade.builder().assessmentId(3L).studentId(studentId).value(BigDecimal.valueOf(14)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));
        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(classId, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        ReportCardLine line = lineCaptor.getAllValues().get(0);
        // (12 + 14) / 2 = 13.00 — le QUIZ à 20 (coefficient 5) n'intervient pas du tout.
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("13.00");
        assertThat(line.getCoursAverage()).isNull();
        assertThat(line.getCompoAverage()).isNull();

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        ReportCard rc = rcCaptor.getAllValues().get(rcCaptor.getAllValues().size() - 1);
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.00");
        assertThat(rc.getHonor()).isEqualTo("Passable");
    }

    /**
     * Primaire (§retour pédagogique) : la moyenne générale est la moyenne SIMPLE des moyennes de
     * matière (Σ/nombre de matières) — jamais pondérée par le coefficient de la matière
     * (ClasseMatiere), contrairement au Secondaire. Deux matières à coefficients très différents (1 et
     * 10) doivent peser pareil dans la moyenne générale.
     */
    @Test
    void generateForStudent_primaire_overallAverageIgnoresSubjectCoefficients() {
        Long classId = 1L, periodId = 2L, studentId = 10L;
        Long subjectA = 100L, subjectB = 101L;
        Assessment examA = Assessment.builder().id(1L).classId(classId).periodId(periodId)
                .subjectId(subjectA).type(AssessmentType.EXAM).coefficient(BigDecimal.ONE).build();
        Assessment examB = Assessment.builder().id(2L).classId(classId).periodId(periodId)
                .subjectId(subjectB).type(AssessmentType.EXAM).coefficient(BigDecimal.ONE).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(examA, examB));
        // Coefficients de matière très déséquilibrés (1 vs 10) : ne doivent avoir AUCUN effet en primaire.
        when(classSubjectQueryPort.findByClassId(classId)).thenReturn(List.of(
                new ClassSubjectInfo(subjectA, BigDecimal.valueOf(1), 5L),
                new ClassSubjectInfo(subjectB, BigDecimal.valueOf(10), 6L)));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(10)).build()));
        when(gradeRepository.findByAssessmentId(2L)).thenReturn(List.of(
                Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(16)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, periodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));
        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(classId, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));

        service.generateForStudent(studentId, classId, periodId);

        ArgumentCaptor<ReportCard> rcCaptor = ArgumentCaptor.forClass(ReportCard.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).save(rcCaptor.capture());
        ReportCard rc = rcCaptor.getAllValues().get(rcCaptor.getAllValues().size() - 1);
        // (10 + 16) / 2 = 13.00 — une pondération par coefficient (1×10 + 10×16)/11 = 15.45 serait fausse.
        assertThat(rc.getOverallAverage()).isEqualByComparingTo("13.00");
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

    /** Exposition du détail mensuel pour le bulletin (§colonnes par mois, "bulletin-test" de référence). */
    @Test
    void monthlyBreakdown_secondaire_exposesMonthsAndCompositionPerSubject() {
        Long classId = 1L, periodId = 2L, studentId = 10L, subjectId = 100L;
        Assessment octQuiz = Assessment.builder().id(1L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 10)).coefficient(BigDecimal.ONE).build();
        Assessment exam = Assessment.builder().id(2L).classId(classId).periodId(periodId).subjectId(subjectId)
                .type(AssessmentType.EXAM).date(LocalDate.of(2026, 12, 1)).coefficient(BigDecimal.ONE).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, periodId)).thenReturn(List.of(octQuiz, exam));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.valueOf(2), 5L)));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(2L)).thenReturn(List.of(
                Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(16)).build()));
        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(classId, "8ème A", "8ème", "NORMALE", null, null, "COLLEGE")));

        List<gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown> breakdown = service.monthlyBreakdown(studentId, classId, periodId);

        assertThat(breakdown).hasSize(1);
        gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown b = breakdown.get(0);
        assertThat(b.subjectId()).isEqualTo(subjectId);
        assertThat(b.coefficient()).isEqualByComparingTo("2");
        // Coefficient de matière (2) appliqué directement, pas normalisé : 2×12=24.00, 2×16=32.00.
        assertThat(b.monthlyCours()).containsExactly(java.util.Map.entry(java.time.YearMonth.of(2026, 10), new BigDecimal("24.00")));
        assertThat(b.compoAverage()).isEqualByComparingTo("32.00");
        assertThat(b.subjectAverage()).isEqualByComparingTo("28.00"); // (24+32)/2
    }

    @Test
    void monthlyBreakdown_primaire_isAlwaysEmpty() {
        Long classId = 1L, periodId = 2L, studentId = 10L;
        when(academicsDataProvider.findClass(classId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.ClassPublicInfo(classId, "CM2 A", "CM2", "NORMALE", null, null, "PRIMAIRE")));

        List<gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown> breakdown = service.monthlyBreakdown(studentId, classId, periodId);

        assertThat(breakdown).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(assessmentRepository);
    }

    /**
     * Régression (§retour fondateur, école Notre Mère) : certaines écoles séparent la période des
     * évaluations "cours" (ex. "1er Semestre") de celle de sa composition (ex. "Compo 1er Semestre") —
     * deux {@code Periode} distincts liés par {@code compositionPeriodId}, mais un seul bulletin. Les
     * évaluations des DEUX périodes doivent entrer dans le calcul.
     */
    @Test
    void generateForStudent_mergesAssessmentsFromLinkedCompositionPeriod() {
        Long classId = 1L, coursPeriodId = 12L, compoPeriodId = 13L, studentId = 10L, subjectId = 100L;
        Assessment quiz = Assessment.builder().id(1L).classId(classId).periodId(coursPeriodId).subjectId(subjectId)
                .type(AssessmentType.QUIZ).date(LocalDate.of(2026, 10, 10)).coefficient(BigDecimal.ONE).build();
        Assessment exam = Assessment.builder().id(2L).classId(classId).periodId(compoPeriodId).subjectId(subjectId)
                .type(AssessmentType.EXAM).date(LocalDate.of(2027, 1, 15)).coefficient(BigDecimal.ONE).build();
        when(assessmentRepository.findByClassIdAndPeriodId(classId, coursPeriodId)).thenReturn(List.of(quiz));
        when(assessmentRepository.findByClassIdAndPeriodId(classId, compoPeriodId)).thenReturn(List.of(exam));
        when(academicsDataProvider.findPeriod(coursPeriodId)).thenReturn(Optional.of(
                new gn.lacaeducation.academics.PeriodPublicInfo(coursPeriodId, "1er Semestre", "SEMESTRE", null, null, compoPeriodId, null)));
        when(classSubjectQueryPort.findByClassId(classId))
                .thenReturn(List.of(new ClassSubjectInfo(subjectId, BigDecimal.ONE, 5L)));
        when(gradeRepository.findByAssessmentId(1L)).thenReturn(List.of(
                Grade.builder().assessmentId(1L).studentId(studentId).value(BigDecimal.valueOf(12)).build()));
        when(gradeRepository.findByAssessmentId(2L)).thenReturn(List.of(
                Grade.builder().assessmentId(2L).studentId(studentId).value(BigDecimal.valueOf(16)).build()));
        when(reportCardRepository.findByStudentIdAndPeriodId(studentId, coursPeriodId)).thenReturn(Optional.empty());
        when(reportCardRepository.save(any())).thenAnswer(i -> { ReportCard r = i.getArgument(0); if (r.getId() == null) r.setId(99L); return r; });
        when(reportCardRepository.saveLine(any())).thenAnswer(i -> i.getArgument(0));
        when(reportCardRepository.findByClassIdAndPeriodId(classId, coursPeriodId)).thenReturn(List.of());
        when(reportCardRepository.findById(any())).thenAnswer(i -> Optional.of(ReportCard.builder().id(99L).build()));

        service.generateForStudent(studentId, classId, coursPeriodId);

        ArgumentCaptor<ReportCardLine> lineCaptor = ArgumentCaptor.forClass(ReportCardLine.class);
        org.mockito.Mockito.verify(reportCardRepository, atLeastOnce()).saveLine(lineCaptor.capture());
        ReportCardLine line = lineCaptor.getAllValues().get(0);
        assertThat(line.getCoursAverage()).isEqualByComparingTo("12.00"); // du périodId demandé
        assertThat(line.getCompoAverage()).isEqualByComparingTo("16.00"); // de la période composition liée
        assertThat(line.getSubjectAverage()).isEqualByComparingTo("14.00"); // (12+16)/2
    }
}
