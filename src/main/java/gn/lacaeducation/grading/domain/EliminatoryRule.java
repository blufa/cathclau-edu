package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Note éliminatoire par matière (§7), paramétrable : si la moyenne de la matière {@code subjectId}
 * est strictement inférieure à {@code threshold}, l'élève reçoit la décision {@code decisionLabel}
 * (ex. « REDOUBLE »), quelle que soit sa moyenne générale.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EliminatoryRule {
    private Long id;
    private Long tenantId;
    private Long subjectId;
    /** Seuil éliminatoire : une moyenne matière strictement inférieure déclenche la règle. */
    private BigDecimal threshold;
    /** Décision imposée en cas de déclenchement (ex. REDOUBLE, AJOURNE). */
    private String decisionLabel;
    private boolean active;
}
