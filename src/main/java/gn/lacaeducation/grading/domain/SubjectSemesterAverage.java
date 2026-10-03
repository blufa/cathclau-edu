package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moyenne semestrielle d'une matière pour un élève — formule confirmée avec la direction pédagogique
 * (features/Calcul-moyenne.pdf, Calcul-moyenne-2.pdf), distincte selon le cycle :
 * <p>
 * <b>Secondaire</b> (Collège/Lycée) :
 * <pre>
 * M_mois      = Σ(coefficient_évaluation × note) / Σ(coefficient_évaluation)   — pour les évaluations
 *               « cours » (QUIZ/HOMEWORK) d'un même mois calendaire (date de l'évaluation)
 * M_cours     = moyenne simple des M_mois des mois ayant au moins une note (mois sans note exclus,
 *               jamais comptés comme 0)
 * M_comp      = Σ(coefficient_évaluation × note) / Σ(coefficient_évaluation)   — pour les évaluations
 *               EXAM (composition) de la période, quel que soit leur nombre
 * M_sem       = (M_cours + M_comp) / 2 ; si l'un des deux est absent (données incomplètes en cours de
 *               période), M_sem retombe sur l'autre seul plutôt que de rester indéfini
 * </pre>
 * FINAL (examen de fin de période) n'entre dans aucun de ces trois calculs — catégorie hors barème
 * semestriel standard.
 * <p>
 * <b>Primaire/maternelle</b> (§retour pédagogique) : moyenne simple (NON pondérée par coefficient,
 * contrairement au Secondaire) des seules compositions (EXAM) du trimestre — {@code Σnote/nombre de
 * compositions} ; les interrogations/devoirs (QUIZ/HOMEWORK) ne comptent pas pour ce cycle.
 */
public final class SubjectSemesterAverage {

    private SubjectSemesterAverage() {
    }

    public record Result(BigDecimal finalValue, BigDecimal coursValue, BigDecimal compoValue) {
    }

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static Result compute(CycleGroup cycleGroup, List<Assessment> assessments, List<Grade> grades) {
        Map<Long, Assessment> byId = new HashMap<>();
        for (Assessment a : assessments) {
            byId.put(a.getId(), a);
        }

        List<WeightedNote> noted = new ArrayList<>();
        for (Grade g : grades) {
            if (g.getValue() == null) continue;
            Assessment a = byId.get(g.getAssessmentId());
            if (a == null) continue; // note orpheline (incohérence appelant) : ignorée, pas d'exception
            noted.add(new WeightedNote(a, g.getValue()));
        }

        if (cycleGroup == CycleGroup.PRIMAIRE) {
            List<WeightedNote> compositions = noted.stream()
                    .filter(n -> n.assessment.getType() == AssessmentType.EXAM)
                    .toList();
            BigDecimal simple = simpleAverage(compositions);
            return new Result(simple, null, null);
        }

        List<WeightedNote> cours = noted.stream()
                .filter(n -> n.assessment.getType() == AssessmentType.QUIZ
                        || n.assessment.getType() == AssessmentType.HOMEWORK)
                .toList();
        List<WeightedNote> compo = noted.stream()
                .filter(n -> n.assessment.getType() == AssessmentType.EXAM)
                .toList();

        BigDecimal mCours = monthlyAverage(cours);
        BigDecimal mComp = weightedAverage(compo);
        BigDecimal mSem = combine(mCours, mComp);

        return new Result(mSem, mCours, mComp);
    }

    /** M_cours : moyenne simple des moyennes mensuelles pondérées, mois sans note exclus. */
    private static BigDecimal monthlyAverage(List<WeightedNote> cours) {
        Map<YearMonth, List<WeightedNote>> byMonth = new HashMap<>();
        for (WeightedNote n : cours) {
            if (n.assessment.getDate() == null) continue; // pas de mois connu : exclue, pas d'exception
            byMonth.computeIfAbsent(YearMonth.from(n.assessment.getDate()), k -> new ArrayList<>()).add(n);
        }
        if (byMonth.isEmpty()) return null;

        BigDecimal sum = BigDecimal.ZERO;
        int monthCount = 0;
        for (List<WeightedNote> monthNotes : byMonth.values()) {
            BigDecimal monthAverage = weightedAverage(monthNotes);
            if (monthAverage == null) continue;
            sum = sum.add(monthAverage);
            monthCount++;
        }
        if (monthCount == 0) return null;
        return sum.divide(BigDecimal.valueOf(monthCount), SCALE, ROUNDING);
    }

    /** {@code Σ(note) / nombre de notes} — jamais pondérée par coefficient (Primaire, §retour
     * pédagogique) ; {@code null} si vide. */
    private static BigDecimal simpleAverage(List<WeightedNote> notes) {
        if (notes.isEmpty()) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (WeightedNote n : notes) {
            sum = sum.add(n.value);
        }
        return sum.divide(BigDecimal.valueOf(notes.size()), SCALE, ROUNDING);
    }

    /** {@code Σ(coefficient × note) / Σ(coefficient)}, coefficient absent = 1 ; {@code null} si vide. */
    private static BigDecimal weightedAverage(List<WeightedNote> notes) {
        if (notes.isEmpty()) return null;
        BigDecimal sumWeighted = BigDecimal.ZERO;
        BigDecimal sumCoef = BigDecimal.ZERO;
        for (WeightedNote n : notes) {
            BigDecimal coef = n.assessment.getCoefficient() != null ? n.assessment.getCoefficient() : BigDecimal.ONE;
            sumWeighted = sumWeighted.add(n.value.multiply(coef));
            sumCoef = sumCoef.add(coef);
        }
        if (sumCoef.compareTo(BigDecimal.ZERO) == 0) return null;
        return sumWeighted.divide(sumCoef, SCALE, ROUNDING);
    }

    /** M_sem = (M_cours + M_comp) / 2, ou l'un des deux seul si l'autre est absent. */
    private static BigDecimal combine(BigDecimal cours, BigDecimal comp) {
        if (cours != null && comp != null) {
            return cours.add(comp).divide(BigDecimal.valueOf(2), SCALE, ROUNDING);
        }
        return cours != null ? cours : comp;
    }

    private record WeightedNote(Assessment assessment, BigDecimal value) {
    }
}
