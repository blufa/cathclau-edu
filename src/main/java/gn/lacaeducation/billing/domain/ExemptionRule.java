package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Type d'exonération pré-configuré et réutilisable (ex. "Orphelin — 100%", "Réduction sociale — 50%",
 * §retour comptable, refonte inscription) — le comptable configure une fois chaque cas de figure ici,
 * puis le sélectionne dans une liste déroulante à l'inscription au lieu de ressaisir type/pourcentage/
 * frais concernés à chaque élève. Distinct de {@link Exemption}, qui reste l'exonération réellement
 * accordée à UN élève précis (créée à partir d'une règle, ou saisie librement au cas par cas).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExemptionRule {
    private Long id;
    private Long tenantId;
    private String label;
    private ExemptionType type;
    /** Pourcentage [0,100] pour TOTAL/PARTIAL (TOTAL = 100). */
    private BigDecimal percentage;
    /** Montant fixe pour une remise EXCEPTIONAL. */
    private BigDecimal amount;
    /** Frais concernés — un seul, plusieurs, ou vide/null = tous les frais. */
    private List<FeeType> feeTypes;
    /** Vrai si ce type représente un avantage personnel (enfant de membre du personnel) — propagé sur
     * l'{@link Exemption} créée, pour que la retenue sur salaire (§payroll) continue de fonctionner. */
    private boolean staffBenefit;
    private boolean active;
}
