package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Politique de prise en charge des enfants du personnel (§6), paramétrable par établissement :
 * les {@code coveredChildrenCount} premiers enfants (les plus âgés) d'un employé bénéficient de
 * {@code coveragePercent} % de prise en charge sur le frais {@code feeType}.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffChildBenefitPolicy {
    private Long id;
    private Long tenantId;
    private String label;
    /** Nombre d'enfants pris en charge (ex. 1 = seul le premier enfant). */
    private Integer coveredChildrenCount;
    /** Pourcentage de prise en charge [0,100] (100 = entièrement pris en charge). */
    private BigDecimal coveragePercent;
    /** Frais concerné (null = tous les frais). */
    private FeeType feeType;
    private boolean active;
}
