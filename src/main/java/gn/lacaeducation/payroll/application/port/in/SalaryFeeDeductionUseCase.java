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

    /** Détail de la retenue "avantage personnel" d'un salarié, enfant par enfant (§retour comptable :
     * "détailler bien les retenues sur le modal du détail de la fiche, afficher pour chaque enfant les
     * factures sur lesquelles tu as retenu") — inclut aussi les enfants dont le parent a choisi de payer
     * lui-même ({@code optedOut}, retenue déjà désactivée) et les frais facultatifs du catalogue de leur
     * classe pas encore facturés (informatif, ils entreront automatiquement en retenue s'ils sont
     * facturés plus tard). */
    record ChildDeductionDetail(Long studentId, String studentName, boolean optedOut,
                                 List<gn.lacaeducation.billing.StudentFeePaymentApi.DeductedInvoiceLine> deductedInvoices,
                                 List<gn.lacaeducation.billing.StudentFeePaymentApi.OptionalFeeNotice> optionalFeesNotYetInvoiced) {}

    List<ChildDeductionDetail> staffBenefitDetail(Long tenantId, EmployeeType employeeType, Long staffId,
                                                   LocalDate periodStart, String periodLabel);

    /** Exclut (ou réintègre) spécifiquement UN enfant de la retenue — par enfant, contrairement au
     * réglage global (parent "paie lui-même pour tous ses enfants") : un même salarié peut avoir un
     * enfant à charge de l'avantage personnel et un autre payé directement par un parent (§retour
     * comptable : "il y a des enfants qui sont [à charge] et d'autres [payés directement]"). */
    void setChildStaffDeductionExcluded(Long tenantId, EmployeeType employeeType, Long staffId, Long studentId, boolean excluded);
}
