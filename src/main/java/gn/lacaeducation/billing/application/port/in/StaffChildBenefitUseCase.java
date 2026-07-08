package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;

import java.math.BigDecimal;
import java.util.List;

public interface StaffChildBenefitUseCase {
    record PolicyCommand(String label, Integer coveredChildrenCount, BigDecimal coveragePercent,
                         FeeType feeType, Boolean active) {}

    /** Prise en charge applicable à un enfant d'employé pour un frais donné. */
    record StaffCoverage(BigDecimal coveragePercent, String policyLabel) {}

    StaffChildBenefitPolicy create(Long tenantId, PolicyCommand command);
    StaffChildBenefitPolicy update(Long id, PolicyCommand command);
    List<StaffChildBenefitPolicy> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la prise en charge école pour {@code studentId} si un de ses parents est employé (0 % sinon). */
    StaffCoverage computeCoverage(Long tenantId, Long studentId, FeeType feeType);
}
