package gn.lacaeducation.grading.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Formule secondaire (Collège/Lycée) confirmée avec la direction (features/Calcul-moyenne.pdf,
 * Calcul-moyenne-2.pdf) : M_sem = (M_cours + M_comp) / 2 ; M_cours = moyenne des moyennes mensuelles
 * (QUIZ/HOMEWORK, regroupées par mois calendaire de la date de l'évaluation, mois sans note exclus) ;
 * M_comp = moyenne pondérée des évaluations EXAM (composition). FINAL est hors de cette formule.
 * Primaire/maternelle : moyenne pondérée simple, tous types confondus, sans découpage.
 */
class SubjectSemesterAverageTest {

    private long nextId = 1;

    private Assessment assessment(AssessmentType type, String date, String coef) {
        return Assessment.builder().id(nextId++).type(type)
                .date(date == null ? null : LocalDate.parse(date))
                .coefficient(coef == null ? null : new BigDecimal(coef))
                .build();
    }

    private Grade grade(Assessment a, String value) {
        return Grade.builder().assessmentId(a.getId()).studentId(1L).value(new BigDecimal(value)).build();
    }

    @Test
    void secondaire_monthlyAverage_isWeightedByEvaluationCoefficient() {
        Assessment q1 = assessment(AssessmentType.QUIZ, "2026-10-05", "1");
        Assessment q2 = assessment(AssessmentType.QUIZ, "2026-10-20", "3");
        List<Assessment> assessments = List.of(q1, q2);
        List<Grade> grades = List.of(grade(q1, "10"), grade(q2, "16"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        // M_oct = (10×1 + 16×3) / (1+3) = 58/4 = 14.50 ; pas de composition → M_sem = M_cours
        assertThat(r.coursValue()).isEqualByComparingTo("14.50");
        assertThat(r.compoValue()).isNull();
        assertThat(r.finalValue()).isEqualByComparingTo("14.50");
    }

    @Test
    void secondaire_coursIsAverageOfMonthlyAverages_notFlatAverageOfAllNotes() {
        // Octobre : une seule note à 20. Novembre : deux notes à 0 et 0.
        // Moyenne à plat de toutes les notes = (20+0+0)/3 = 6.67 — CE N'EST PAS le résultat attendu.
        // Moyenne des moyennes mensuelles = (M_oct=20 + M_nov=0) / 2 = 10.00 — chaque mois pèse pareil.
        Assessment oct = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        Assessment nov1 = assessment(AssessmentType.HOMEWORK, "2026-11-03", "1");
        Assessment nov2 = assessment(AssessmentType.HOMEWORK, "2026-11-18", "1");
        List<Assessment> assessments = List.of(oct, nov1, nov2);
        List<Grade> grades = List.of(grade(oct, "20"), grade(nov1, "0"), grade(nov2, "0"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void secondaire_monthsWithoutAnyCoursNote_areExcludedNotCountedAsZero() {
        // Seul octobre a une note cours (décembre n'a encore rien de saisi, année en cours).
        Assessment oct = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        List<Assessment> assessments = List.of(oct);
        List<Grade> grades = List.of(grade(oct, "12"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        // Si décembre comptait pour 0, M_cours serait tiré vers le bas — il doit valoir 12 pile.
        assertThat(r.coursValue()).isEqualByComparingTo("12.00");
    }

    @Test
    void secondaire_compoIsWeightedAverageOfExamAssessments() {
        Assessment compo1 = assessment(AssessmentType.EXAM, "2026-12-01", "1");
        Assessment compo2 = assessment(AssessmentType.EXAM, "2026-12-15", "1");
        List<Assessment> assessments = List.of(compo1, compo2);
        List<Grade> grades = List.of(grade(compo1, "14"), grade(compo2, "16"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.compoValue()).isEqualByComparingTo("15.00");
        assertThat(r.coursValue()).isNull();
        assertThat(r.finalValue()).isEqualByComparingTo("15.00");
    }

    @Test
    void secondaire_semesterAverage_isFiftyFiftyCoursAndCompo() {
        Assessment oct = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        Assessment compo = assessment(AssessmentType.EXAM, "2026-12-01", "1");
        List<Assessment> assessments = List.of(oct, compo);
        List<Grade> grades = List.of(grade(oct, "10"), grade(compo, "16"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("10.00");
        assertThat(r.compoValue()).isEqualByComparingTo("16.00");
        assertThat(r.finalValue()).isEqualByComparingTo("13.00"); // (10+16)/2
    }

    @Test
    void secondaire_missingCompo_fallsBackToCoursAlone() {
        Assessment oct = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        List<Assessment> assessments = List.of(oct);
        List<Grade> grades = List.of(grade(oct, "11"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.finalValue()).isEqualByComparingTo("11.00");
    }

    @Test
    void secondaire_missingCours_fallsBackToCompoAlone() {
        Assessment compo = assessment(AssessmentType.EXAM, "2026-12-01", "1");
        List<Assessment> assessments = List.of(compo);
        List<Grade> grades = List.of(grade(compo, "17"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.finalValue()).isEqualByComparingTo("17.00");
    }

    @Test
    void secondaire_noGradesAtAll_returnsNullEverywhere() {
        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, List.of(), List.of());

        assertThat(r.finalValue()).isNull();
        assertThat(r.coursValue()).isNull();
        assertThat(r.compoValue()).isNull();
    }

    @Test
    void secondaire_finalTypeAssessments_areExcludedFromCoursAndCompo() {
        // FINAL (examen de fin de période) est une catégorie à part, hors barème semestriel standard.
        Assessment quiz = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        Assessment finalExam = assessment(AssessmentType.FINAL, "2026-12-20", "1");
        List<Assessment> assessments = List.of(quiz, finalExam);
        List<Grade> grades = List.of(grade(quiz, "10"), grade(finalExam, "20"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("10.00");
        assertThat(r.compoValue()).isNull();
        assertThat(r.finalValue()).isEqualByComparingTo("10.00"); // le FINAL (20) n'entre nulle part
    }

    @Test
    void secondaire_coursNoteWithoutDate_isExcludedFromMonthlyGrouping() {
        // Une note « cours » sans date ne peut être rattachée à aucun mois : exclue de M_cours, pas
        // d'exception levée (donnée incomplète, ne doit pas casser le calcul des autres matières/élèves).
        Assessment dated = assessment(AssessmentType.QUIZ, "2026-10-10", "1");
        Assessment undated = assessment(AssessmentType.QUIZ, null, "1");
        List<Assessment> assessments = List.of(dated, undated);
        List<Grade> grades = List.of(grade(dated, "12"), grade(undated, "0"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("12.00");
    }

    @Test
    void secondaire_missingEvaluationCoefficient_defaultsToOne() {
        Assessment a1 = assessment(AssessmentType.QUIZ, "2026-10-05", null);
        Assessment a2 = assessment(AssessmentType.QUIZ, "2026-10-20", null);
        List<Assessment> assessments = List.of(a1, a2);
        List<Grade> grades = List.of(grade(a1, "10"), grade(a2, "14"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("12.00"); // (10+14)/2, coefficients=1
    }

    @Test
    void primaire_isSimpleWeightedAverageAcrossAllTypes_noCoursCompoSplit() {
        Assessment quiz = assessment(AssessmentType.QUIZ, "2026-10-05", "1");
        Assessment compo = assessment(AssessmentType.EXAM, "2026-12-01", "3");
        List<Assessment> assessments = List.of(quiz, compo);
        List<Grade> grades = List.of(grade(quiz, "10"), grade(compo, "14"));

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.PRIMAIRE, assessments, grades);

        // (10×1 + 14×3) / (1+3) = 52/4 = 13.00, aucune séparation mensuelle ni cours/compo.
        assertThat(r.finalValue()).isEqualByComparingTo("13.00");
        assertThat(r.coursValue()).isNull();
        assertThat(r.compoValue()).isNull();
    }

    @Test
    void primaire_noGrades_returnsNull() {
        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.PRIMAIRE, List.of(), List.of());

        assertThat(r.finalValue()).isNull();
    }

    @Test
    void gradesReferencingUnknownAssessment_areIgnored() {
        // Garde-fou : une note dont l'évaluation n'est pas dans la liste fournie est ignorée plutôt que
        // de lever une exception (ne devrait jamais arriver en pratique, l'appelant construit les deux
        // listes ensemble — mais un calcul de bulletin ne doit jamais planter sur une incohérence).
        Assessment known = assessment(AssessmentType.QUIZ, "2026-10-05", "1");
        List<Assessment> assessments = List.of(known);
        List<Grade> grades = new ArrayList<>(List.of(grade(known, "10")));
        grades.add(Grade.builder().assessmentId(9999L).studentId(1L).value(new BigDecimal("99")).build());

        SubjectSemesterAverage.Result r = SubjectSemesterAverage.compute(CycleGroup.SECONDAIRE, assessments, grades);

        assertThat(r.coursValue()).isEqualByComparingTo("10.00");
    }
}
