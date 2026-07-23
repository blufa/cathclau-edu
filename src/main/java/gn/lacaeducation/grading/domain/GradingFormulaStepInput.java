package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Une entrée d'une étape {@link StepKind#COMBINE} : référence le résultat d'une étape précédente
 * (par son {@code stepOrder} au sein de la même {@link GradingFormula}) et le poids à lui appliquer.
 * Chaque entrée a son propre {@link WeightMode} — c'est ce qui permet, par exemple, de donner à la
 * « moyenne de cours » un poids de 1 et à la « moyenne de composition » un poids égal au coefficient
 * de la matière, dans la même étape « moyenne semestrielle ».
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingFormulaStepInput {
    private Long id;
    /** {@code stepOrder} de l'étape référencée (doit précéder l'étape courante dans la formule). */
    private Integer refStepOrder;
    private WeightMode weightMode;
    /** Poids utilisé uniquement quand {@link #weightMode} vaut {@code FIXED}. */
    private BigDecimal fixedWeight;
}
