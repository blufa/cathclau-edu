package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Moyenne semestrielle d'une matière pour un élève — formule confirmée avec la direction pédagogique
 * (features/Calcul-moyenne.pdf, Calcul-moyenne-2.pdf, features/exemple-pour-un-eleve-college.pdf),
 * distincte selon le cycle :
 * <p>
 * <b>Secondaire</b> (Collège/Lycée) : le coefficient de la matière (configuré une fois sur
 * Classes → Matières, {@code ClassSubjectInfo.coefficient}) multiplie directement les notes brutes
 * sur 20 — il n'est PAS normalisé à ce stade (vérifié chiffre par chiffre sur l'exemple fourni : un
 * élève à 14 à l'oral et 15 à l'écrit en coefficient 2 donne M_mois = (2×14 + 2×15)/2 = 29, une valeur
 * "gonflée" par le coefficient, pas une note sur 20). Le coefficient par évaluation
 * ({@code Assessment.coefficient}) n'est plus utilisé ici : utiliser systématiquement le coefficient
 * de la matière évite tout risque de décalage si une évaluation a été créée avec un coefficient
 * différent de celui configuré sur la matière (incident vécu, école Notre Mère, Anglais coef 1 en
 * configuration mais coef 2 sur les évaluations).
 * <pre>
 * M_mois      = coefficient_matière × (Σ notes « cours » du mois / nombre de notes du mois)
 *               — pour les évaluations « cours » (QUIZ/HOMEWORK) d'un même mois calendaire
 * M_cours     = moyenne simple des M_mois des mois ayant au moins une note (mois sans note exclus,
 *               jamais comptés comme 0)
 * M_comp      = coefficient_matière × (Σ notes de composition / nombre de notes de composition)
 *               — pour les évaluations EXAM (composition) de la période, quel que soit leur nombre
 * M_sem       = (M_cours + M_comp) / 2 ; si l'un des deux est absent (données incomplètes en cours de
 *               période), M_sem retombe sur l'autre seul plutôt que de rester indéfini
 * </pre>
 * M_sem est donc déjà "coefficiée" — la moyenne générale de l'élève (hors de cette classe, voir
 * {@code GradeCalculationService}) se contente de sommer les M_sem de toutes les matières et de
 * diviser par la somme des coefficients, SANS reproduire la multiplication (déjà faite ici).
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

    /** @param monthlyCours moyenne "cours" (QUIZ/HOMEWORK), déjà multipliée par le coefficient de la
     * matière, de chaque mois calendaire ayant au moins une note — jamais nul, vide en Primaire (pas de
     * notion de "cours" dans ce cycle, voir la javadoc de la classe) ou si aucune évaluation "cours" n'a
     * de date/note exploitable. Pour l'affichage détaillé du bulletin (colonnes par mois) — n'intervient
     * dans aucun calcul de moyenne, {@code coursValue} reste la seule source de vérité pour M_cours. */
    public record Result(BigDecimal finalValue, BigDecimal coursValue, BigDecimal compoValue,
                         SortedMap<YearMonth, BigDecimal> monthlyCours) {
    }

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** @param subjectCoefficient coefficient de la matière (Secondaire uniquement — ignoré en
     * Primaire, où le coefficient n'entre jamais en jeu) ; absent = 1. */
    public static Result compute(CycleGroup cycleGroup, BigDecimal subjectCoefficient,
                                  List<Assessment> assessments, List<Grade> grades) {
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
            return new Result(simple, null, null, new TreeMap<>());
        }

        BigDecimal coef = subjectCoefficient != null ? subjectCoefficient : BigDecimal.ONE;

        List<WeightedNote> cours = noted.stream()
                .filter(n -> n.assessment.getType() == AssessmentType.QUIZ
                        || n.assessment.getType() == AssessmentType.HOMEWORK)
                .toList();
        List<WeightedNote> compo = noted.stream()
                .filter(n -> n.assessment.getType() == AssessmentType.EXAM)
                .toList();

        SortedMap<YearMonth, BigDecimal> monthlyCours = monthlyBreakdown(cours, coef);
        BigDecimal mCours = monthlyAverage(monthlyCours);
        BigDecimal mComp = scaledAverage(compo, coef);
        BigDecimal mSem = combine(mCours, mComp);

        return new Result(mSem, mCours, mComp, monthlyCours);
    }

    /** {@code coefficient_matière × (Σ notes du mois / nombre de notes)} de chaque mois calendaire ayant
     * au moins une note — mois sans note absents de la carte (jamais une entrée à {@code null}/zéro). */
    private static SortedMap<YearMonth, BigDecimal> monthlyBreakdown(List<WeightedNote> cours, BigDecimal coef) {
        Map<YearMonth, List<WeightedNote>> byMonth = new HashMap<>();
        for (WeightedNote n : cours) {
            if (n.assessment.getDate() == null) continue; // pas de mois connu : exclue, pas d'exception
            byMonth.computeIfAbsent(YearMonth.from(n.assessment.getDate()), k -> new ArrayList<>()).add(n);
        }
        SortedMap<YearMonth, BigDecimal> result = new TreeMap<>();
        for (Map.Entry<YearMonth, List<WeightedNote>> entry : byMonth.entrySet()) {
            BigDecimal monthAverage = scaledAverage(entry.getValue(), coef);
            if (monthAverage != null) result.put(entry.getKey(), monthAverage);
        }
        return result;
    }

    /** M_cours : moyenne simple des moyennes mensuelles déjà calculées, {@code null} si aucune. */
    private static BigDecimal monthlyAverage(SortedMap<YearMonth, BigDecimal> monthlyCours) {
        if (monthlyCours.isEmpty()) return null;
        BigDecimal sum = monthlyCours.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(monthlyCours.size()), SCALE, ROUNDING);
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

    /** {@code coefficient_matière × (Σ note / nombre de notes)} (Secondaire, Calcul-moyenne-2.pdf) ;
     * {@code null} si vide. Un seul arrondi final pour éviter toute dérive. */
    private static BigDecimal scaledAverage(List<WeightedNote> notes, BigDecimal coef) {
        if (notes.isEmpty()) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (WeightedNote n : notes) {
            sum = sum.add(n.value);
        }
        return sum.multiply(coef).divide(BigDecimal.valueOf(notes.size()), SCALE, ROUNDING);
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
