package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;

import java.util.List;
import java.util.Optional;

public interface StaffChildBenefitPolicyRepository {
    StaffChildBenefitPolicy save(StaffChildBenefitPolicy policy);
    Optional<StaffChildBenefitPolicy> findById(Long id);
    List<StaffChildBenefitPolicy> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
