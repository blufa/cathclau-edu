package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Règle de remise pour paiement annuel complet paramétrable (§billing) : « si le parent règle en un
 * seul paiement la totalité des tranches restantes d'un frais, appliquer {@code percentage} % sur
 * chaque facture couverte ». Aucun taux codé en dur : tout est configuré par l'établissement.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EarlyPaymentDiscountRule {
    private Long id;
    private Long tenantId;
    private String label;
    /** Pourcentage de réduction [0, 100] (100 = gratuité). */
    private BigDecimal percentage;
    /** Frais concerné (null = tous les frais). */
    private FeeType feeType;
    private boolean active;
}
