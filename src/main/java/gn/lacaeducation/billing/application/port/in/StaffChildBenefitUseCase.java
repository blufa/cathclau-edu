package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;

import java.math.BigDecimal;
import java.util.List;

public interface StaffChildBenefitUseCase {
    /** {@code feeTypes} : un seul type, plusieurs, ou vide/null = tous les frais. */
    record PolicyCommand(String label, Integer coveredChildrenCount, BigDecimal coveragePercent,
                         List<FeeType> feeTypes, Boolean active) {}

    /** Prise en charge applicable à un enfant d'employé pour un frais donné. */
    record StaffCoverage(BigDecimal coveragePercent, String policyLabel) {}

    /** Prise en charge d'un élève précis, pour affichage en liste (badge). */
    record StudentCoverage(Long studentId, BigDecimal coveragePercent, String policyLabel) {}

    StaffChildBenefitPolicy create(Long tenantId, PolicyCommand command);
    StaffChildBenefitPolicy update(Long id, PolicyCommand command);
    List<StaffChildBenefitPolicy> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la prise en charge école pour {@code studentId} si un de ses parents est employé (0 % sinon). */
    StaffCoverage computeCoverage(Long tenantId, Long studentId, FeeType feeType);

    /** Parmi {@code studentIds}, ceux qui bénéficient d'une prise en charge école non nulle (badge liste des élèves). */
    List<StudentCoverage> computeCoverageBatch(Long tenantId, List<Long> studentIds, FeeType feeType);
}
