package gn.lacaeducation.grading.domain;

import lombok.*;

import java.util.List;

/**
 * Une étape ordonnée d'une {@link GradingFormula}. Selon {@link #stepKind} :
 * <ul>
 *   <li>{@code GROUP} : moyenne des évaluations correspondant à {@link #monthIndex} (n-ième mois de
 *       la période) ou {@link #assessmentTypeId} (ex. le type « Composition ») — ni l'un ni l'autre
 *       renseigné signifie « toutes les évaluations de la période » (formule héritée, §migration) ;</li>
 *   <li>{@code COMBINE} : moyenne pondérée des résultats d'étapes précédentes, via {@link #inputs}.</li>
 * </ul>
 * L'étape marquée {@link #finalStep} fournit le résultat final de la formule (la moyenne de matière).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingFormulaStep {
    private Long id;
    private Long formulaId;
    /** Ordre d'exécution, unique au sein d'une formule ; référencé par {@link GradingFormulaStepInput#getRefStepOrder()}. */
    private int stepOrder;
    private String label;
    private StepKind stepKind;

    // -- GROUP --
    /** N-ième mois de la période (1 = premier mois), relatif à sa date de début. */
    private Integer monthIndex;
    /** Type d'évaluation ciblé (ex. « Composition »), référence {@code AssessmentTypeConfig}. */
    private Long assessmentTypeId;

    // -- COMBINE --
    private List<GradingFormulaStepInput> inputs;

    private boolean finalStep;
}
