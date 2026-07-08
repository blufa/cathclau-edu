package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase.StaffCoverage;
import gn.lacaeducation.billing.application.port.out.StaffChildBenefitPolicyRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.StaffChildBenefitPolicy;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffChildBenefitServiceTest {

    @Mock StaffChildBenefitPolicyRepository repo;
    @Mock ParentAccessProvider parentAccessProvider;
    @Mock TeacherPublicInfoProvider teacherProvider;
    @Mock FamilyProvider familyProvider;
    @InjectMocks StaffChildBenefitService service;

    private StaffChildBenefitPolicy policyFirstChildFullyCovered() {
        return StaffChildBenefitPolicy.builder().id(1L).label("Personnel — 1er enfant")
                .coveredChildrenCount(1).coveragePercent(new BigDecimal("100")).feeType(FeeType.MONTHLY).active(true).build();
    }

    private void staffParentWithTwoChildren() {
        when(parentAccessProvider.findParentUserIdsByStudent(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.of(9L)); // parent = employé
        when(repo.findByTenantId(1L)).thenReturn(List.of(policyFirstChildFullyCovered()));
        lenient().when(familyProvider.findSiblings(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of(
                new ChildInfo(1L, LocalDate.of(2010, 1, 1)),   // aîné
                new ChildInfo(2L, LocalDate.of(2015, 1, 1))));  // cadet
    }

    @Test
    void computeCoverage_firstChildOfStaff_fullyCovered() {
        staffParentWithTwoChildren();

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("100");
        assertThat(c.policyLabel()).contains("Personnel");
    }

    @Test
    void computeCoverage_secondChildOfStaff_notCovered() {
        staffParentWithTwoChildren();

        StaffCoverage c = service.computeCoverage(1L, 2L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("0");
    }

    @Test
    void computeCoverage_notStaffChild_returnsZero() {
        when(parentAccessProvider.findParentUserIdsByStudent(1L)).thenReturn(List.of(50L));
        when(teacherProvider.findTeacherIdByUserId(50L)).thenReturn(Optional.empty()); // pas employé

        StaffCoverage c = service.computeCoverage(1L, 1L, FeeType.MONTHLY);

        assertThat(c.coveragePercent()).isEqualByComparingTo("0");
    }
}
