package gn.lacaeducation.payroll.application.port.in;

import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SalaryFeeDeductionUseCase {
    record RuleCommand(String label, BigDecimal amountPerChild, Integer maxChildren, BigDecimal cap, Boolean active) {}

    /** Retenue de frais scolaires calculée pour un employé. */
    record FeeDeduction(BigDecimal amount, String label, int childrenCount) {}

    SalaryFeeDeductionRule create(Long tenantId, RuleCommand command);
    SalaryFeeDeductionRule update(Long id, RuleCommand command);
    List<SalaryFeeDeductionRule> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la retenue de frais scolaires pour un salarié — enseignant ou personnel administratif/
     * direction/support, {@code employeeType} détermine dans quel référentiel {@code staffId} est
     * résolu (0 s'il n'a pas d'enfant ou pas de règle). */
    FeeDeduction computeFeeDeduction(Long tenantId, EmployeeType employeeType, Long staffId);

    /**
     * Répercute la retenue de scolarité — déjà appliquée comme ligne de paie à la génération de la
     * fiche (voir {@link #computeFeeDeduction}) — comme paiement sur les factures de mensualité des
     * enfants concernés, au moment où la fiche est réellement PAYÉE (§payroll ↔ billing) : pour que la
     * facturation ne continue pas d'afficher ces enfants comme débiteurs alors que l'école a déjà été
     * payée, via la paie plutôt que le guichet. Recalcule qui est concerné et pour quel montant (même
     * logique que {@link #computeFeeDeduction}) ; sans effet si aucune retenue n'est applicable.
     */
    void applyDeductionAsInvoicePayment(Long tenantId, EmployeeType employeeType, Long staffId);

    /** Solde non couvert par un avantage personnel PARTIEL (§billing), pour le mois de paie
     * {@code periodStart}/{@code periodLabel} uniquement, sur les factures des enfants scolarisés de
     * cet employé — le « reste » à retenir sur son salaire CE mois-ci. Distinct de
     * {@link #computeFeeDeduction} (règle de paie à montant fixe par enfant, indépendante du montant
     * réel dû) : ici le montant est exactement celui qui reste à charge de la famille pour ce mois,
     * connu par la facturation — jamais la somme de tous les mois déjà générés d'avance (§retour
     * comptable, bug corrigé : une mensualité éclatée en 9 factures dès l'inscription ne doit faire
     * retenir qu'UN mois par fiche de paie). 0 si aucun de ses enfants n'a de facture concernée ce
     * mois-ci. */
    record StaffBenefitRemainderDeduction(BigDecimal amount, int childrenCount) {}

    StaffBenefitRemainderDeduction computeStaffBenefitRemainder(Long tenantId, EmployeeType employeeType, Long staffId,
                                                                 LocalDate periodStart, String periodLabel);

    /** Répercute {@link #computeStaffBenefitRemainder} comme paiement sur les factures du même mois
     * (même portée), au moment où la fiche de paie est réellement PAYÉE (même principe que
     * {@link #applyDeductionAsInvoicePayment}). */
    void applyStaffBenefitRemainderAsInvoicePayment(Long tenantId, EmployeeType employeeType, Long staffId,
                                                     LocalDate periodStart, String periodLabel);
}
