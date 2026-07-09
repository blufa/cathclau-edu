package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Une règle d'une structure salariale (inspiré OpenEduCat) : nom, code unique, catégorie (Basic,
 * Allowance, Deduction…), gain ou retenue, calcul en montant fixe ou en pourcentage, valeur.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRule {
    private Long id;
    private Long tenantId;
    private Long structureId;
    private String label;
    private String code;
    private String category;
    private PayslipLineType lineType;
    private RuleComputation computation;
    private BigDecimal value;
    private Integer sequence;
}
