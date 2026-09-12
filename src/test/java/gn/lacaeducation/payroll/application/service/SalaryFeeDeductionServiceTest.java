package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.FeeDeduction;
import gn.lacaeducation.payroll.application.port.out.SalaryFeeDeductionRuleRepository;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryFeeDeductionServiceTest {

    @Mock SalaryFeeDeductionRuleRepository repo;
    @Mock TeacherPublicInfoProvider teacherProvider;
    @Mock ParentAccessProvider parentAccessProvider;
    @InjectMocks SalaryFeeDeductionService service;

    private SalaryFeeDeductionRule rule(String perChild, Integer maxChildren, String cap) {
        return SalaryFeeDeductionRule.builder().id(1L).label("Retenue frais scolaires")
                .amountPerChild(new BigDecimal(perChild)).maxChildren(maxChildren)
                .cap(cap == null ? null : new BigDecimal(cap)).active(true).build();
    }

    private void staffWithTwoChildren() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of(1L, 2L));
    }

    @Test
    void computeFeeDeduction_perChildAmount_timesChildren() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("100000");
        assertThat(d.childrenCount()).isEqualTo(2);
    }

    @Test
    void computeFeeDeduction_maxChildrenLimitsConcerned() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", 1, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("50000");
        assertThat(d.childrenCount()).isEqualTo(1);
    }

    @Test
    void computeFeeDeduction_capApplies() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, "30000")));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("30000");
    }

    @Test
    void computeFeeDeduction_noChildren_returnsZero() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of());

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("0");
    }

    @Test
    void computeFeeDeduction_onlyCountsChildrenWithValidatedEnrollment() {
        // L'adaptateur exclut déjà les enfants sans inscription VALIDÉE (abandon, annulée, jamais
        // inscrite) : le service ne voit que ceux qui restent, ici un seul au lieu de deux.
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of(1L));
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("50000");
        assertThat(d.childrenCount()).isEqualTo(1);
    }

    @Test
    void computeFeeDeduction_noRule_returnsZero() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of());

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("0");
    }
}
