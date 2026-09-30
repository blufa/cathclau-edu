package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase;
import gn.lacaeducation.billing.application.port.out.StaffChildBenefitPolicyRepository;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import gn.lacaeducation.students.ParentAccessProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StaffChildBenefitService implements StaffChildBenefitUseCase {

    private final StaffChildBenefitPolicyRepository repo;
    private final ParentAccessProvider parentAccessProvider;
    private final TeacherPublicInfoProvider teacherProvider;
    private final EmployeePublicInfoProvider employeeProvider;
    private final FamilyProvider familyProvider;
    private final gn.lacaeducation.billing.application.port.in.ExemptionUseCase exemptionUseCase;

    @Override
    public StaffChildBenefitPolicy create(Long tenantId, PolicyCommand c) {
        validate(c);
        return repo.save(StaffChildBenefitPolicy.builder()
                .tenantId(tenantId).label(c.label()).coveredChildrenCount(c.coveredChildrenCount())
                .coveragePercent(c.coveragePercent()).feeTypes(c.feeTypes())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public StaffChildBenefitPolicy update(Long id, PolicyCommand c) {
        validate(c);
        StaffChildBenefitPolicy p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffChildBenefitPolicy", id));
        p.setLabel(c.label()); p.setCoveredChildrenCount(c.coveredChildrenCount());
        p.setCoveragePercent(c.coveragePercent()); p.setFeeTypes(c.feeTypes());
        if (c.active() != null) p.setActive(c.active());
        return repo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffChildBenefitPolicy> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCoverage> computeCoverageBatch(Long tenantId, List<Long> studentIds, gn.lacaeducation.billing.domain.FeeType feeType) {
        return studentIds.stream()
                .map(id -> {
                    StaffCoverage c = computeCoverage(tenantId, id, feeType);
                    return new StudentCoverage(id, c.coveragePercent(), c.policyLabel());
                })
                .filter(c -> c.coveragePercent() != null && c.coveragePercent().signum() > 0)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUncoveredStaffChildIds(Long tenantId, List<Long> studentIds) {
        return studentIds.stream()
                .filter(this::isStaffChild)
                .filter(id -> {
                    var exemption = exemptionUseCase.computeApplicable(id, gn.lacaeducation.billing.domain.FeeType.MONTHLY);
                    BigDecimal pct = exemption.percentage() != null ? exemption.percentage() : BigDecimal.ZERO;
                    return pct.signum() <= 0;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStaffChild(Long studentId) {
        // L'un des parents est-il un membre du personnel — enseignant OU personnel non-enseignant
        // (fiche liée au même compte utilisateur que le parent) ?
        return parentAccessProvider.findParentUserIdsByStudent(studentId).stream()
                .anyMatch(uid -> teacherProvider.findTeacherIdByUserId(uid).isPresent()
                        || employeeProvider.findEmployeeIdByUserId(uid).isPresent());
    }

    @Override
    @Transactional(readOnly = true)
    public StaffCoverage computeCoverage(Long tenantId, Long studentId, gn.lacaeducation.billing.domain.FeeType feeType) {
        if (!isStaffChild(studentId)) {
            return new StaffCoverage(BigDecimal.ZERO, null);
        }
        StaffChildBenefitPolicy policy = repo.findByTenantId(tenantId).stream()
                .filter(StaffChildBenefitPolicy::isActive)
                .filter(p -> p.getFeeTypes() == null || p.getFeeTypes().isEmpty() || p.getFeeTypes().contains(feeType))
                .findFirst()
                .orElse(null);
        if (policy == null) {
            return new StaffCoverage(BigDecimal.ZERO, null);
        }
        // Rang de l'enfant parmi la fratrie, l'aîné (plus âgé) d'abord.
        List<ChildInfo> ordered = familyProvider.findSiblings(studentId).stream()
                .sorted(Comparator.comparing(ChildInfo::birthDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int rank = 1;
        for (ChildInfo child : ordered) {
            if (studentId.equals(child.studentId())) break;
            rank++;
        }
        if (policy.getCoveredChildrenCount() != null && rank <= policy.getCoveredChildrenCount()) {
            return new StaffCoverage(policy.getCoveragePercent(), policy.getLabel());
        }
        return new StaffCoverage(BigDecimal.ZERO, null);
    }

    private void validate(PolicyCommand c) {
        if (c.label() == null || c.label().isBlank()) {
            throw new BusinessRuleException("Le libellé de la politique est requis.");
        }
        if (c.coveredChildrenCount() == null || c.coveredChildrenCount() < 1) {
            throw new BusinessRuleException("Le nombre d'enfants pris en charge doit être ≥ 1.");
        }
        if (c.coveragePercent() == null || c.coveragePercent().compareTo(BigDecimal.ZERO) < 0
                || c.coveragePercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Le pourcentage de prise en charge doit être compris entre 0 et 100.");
        }
    }
}
