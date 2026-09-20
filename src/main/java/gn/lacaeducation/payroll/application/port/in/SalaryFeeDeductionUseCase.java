package gn.lacaeducation.payroll.application.port.in;

import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;

import java.math.BigDecimal;
import java.util.List;

public interface SalaryFeeDeductionUseCase {
    record RuleCommand(String label, BigDecimal amountPerChild, Integer maxChildren, BigDecimal cap, Boolean active) {}

    /** Retenue de frais scolaires calculée pour un employé. */
    record FeeDeduction(BigDecimal amount, String label, int childrenCount) {}

    SalaryFeeDeductionRule create(Long tenantId, RuleCommand command);
    SalaryFeeDeductionRule update(Long id, RuleCommand command);
    List<SalaryFeeDeductionRule> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la retenue de frais scolaires pour un employé (0 s'il n'a pas d'enfant ou pas de règle). */
    FeeDeduction computeFeeDeduction(Long tenantId, Long teacherId);

    /**
     * Répercute la retenue de scolarité — déjà appliquée comme ligne de paie à la génération de la
     * fiche (voir {@link #computeFeeDeduction}) — comme paiement sur les factures de mensualité des
     * enfants concernés, au moment où la fiche est réellement PAYÉE (§payroll ↔ billing) : pour que la
     * facturation ne continue pas d'afficher ces enfants comme débiteurs alors que l'école a déjà été
     * payée, via la paie plutôt que le guichet. Recalcule qui est concerné et pour quel montant (même
     * logique que {@link #computeFeeDeduction}) ; sans effet si aucune retenue n'est applicable.
     */
    void applyDeductionAsInvoicePayment(Long tenantId, Long teacherId);

    /** Solde total non couvert par un avantage personnel PARTIEL (§billing) sur les factures des
     * enfants scolarisés de cet employé — le « reste » à retenir sur son salaire. Distinct de
     * {@link #computeFeeDeduction} (règle de paie à montant fixe par enfant, indépendante du montant
     * réel dû) : ici le montant est exactement celui qui reste à charge de la famille, connu par la
     * facturation. 0 si aucun de ses enfants n'a de facture concernée. */
    record StaffBenefitRemainderDeduction(BigDecimal amount, int childrenCount) {}

    StaffBenefitRemainderDeduction computeStaffBenefitRemainder(Long tenantId, Long teacherId);

    /** Répercute {@link #computeStaffBenefitRemainder} comme paiement sur les factures concernées, au
     * moment où la fiche de paie est réellement PAYÉE (même principe que
     * {@link #applyDeductionAsInvoicePayment}). */
    void applyStaffBenefitRemainderAsInvoicePayment(Long tenantId, Long teacherId);
}
