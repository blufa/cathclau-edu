package gn.lacaeducation.payroll.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Règle paramétrable de retenue sur salaire des frais scolaires des enfants du personnel (§7) :
 * {@code amountPerChild} retenu par enfant scolarisé (dans la limite de {@code maxChildren} enfants),
 * plafonné à {@code cap}. Appliquée automatiquement au bulletin de paie de l'employé.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalaryFeeDeductionRule {
    private Long id;
    private Long tenantId;
    private String label;
    /** Montant retenu par enfant concerné. */
    private BigDecimal amountPerChild;
    /** Nombre maximum d'enfants concernés (null = tous les enfants scolarisés). */
    private Integer maxChildren;
    /** Plafond total de la retenue mensuelle (null = pas de plafond). */
    private BigDecimal cap;
    private boolean active;
}
