package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.ApplicableDiscount;
import gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase.RuleCommand;
import gn.lacaeducation.billing.application.port.out.FamilyDiscountRuleRepository;
import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.FamilyProvider.ChildInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyDiscountServiceTest {

    @Mock FamilyDiscountRuleRepository repo;
    @Mock FamilyProvider familyProvider;
    @InjectMocks FamilyDiscountService service;

    // Fratrie de 5 enfants ; le plus jeune (date la plus récente) est l'élève 10.
    private List<ChildInfo> fiveChildren() {
        return List.of(
                new ChildInfo(1L, LocalDate.of(2010, 1, 1)),
                new ChildInfo(2L, LocalDate.of(2012, 1, 1)),
                new ChildInfo(3L, LocalDate.of(2014, 1, 1)),
                new ChildInfo(11L, LocalDate.of(2016, 1, 1)),
                new ChildInfo(10L, LocalDate.of(2019, 1, 1)));
    }

    private FamilyDiscountRule rule5(int min, Integer max, String pct) {
        return FamilyDiscountRule.builder().id(1L).label("Règle fratrie").minChildren(min).maxChildren(max)
                .percentage(new BigDecimal(pct)).feeType(FeeType.MONTHLY).appliesToYoungest(true).active(true).build();
    }

    @Test
    void computeApplicable_exactly5Children_youngestGets5Percent() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        ApplicableDiscount d = service.computeApplicable(1L, 10L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("5");
        assertThat(d.ruleLabel()).isEqualTo("Règle fratrie");
    }

    @Test
    void computeApplicable_notYoungestChild_getsNothing() {
        when(familyProvider.findSiblings(11L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(5, 5, "5")));

        ApplicableDiscount d = service.computeApplicable(1L, 11L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("0");
        assertThat(d.ruleLabel()).isNull();
    }

    @Test
    void computeApplicable_moreThan5Children_youngestFullyExempt() {
        List<ChildInfo> six = new java.util.ArrayList<>(fiveChildren());
        six.add(new ChildInfo(20L, LocalDate.of(2021, 1, 1))); // nouveau plus jeune
        when(familyProvider.findSiblings(20L)).thenReturn(six);
        // Règle « plus de 5 » : min=6, pas de max, 100 %.
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(6, null, "100")));

        ApplicableDiscount d = service.computeApplicable(1L, 20L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("100");
    }

    @Test
    void computeApplicable_noMatchingRule_returnsZero() {
        when(familyProvider.findSiblings(10L)).thenReturn(fiveChildren());
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule5(6, null, "100"))); // exige 6 enfants

        ApplicableDiscount d = service.computeApplicable(1L, 10L, FeeType.MONTHLY);

        assertThat(d.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void create_percentageOver100_throws() {
        assertThatThrownBy(() -> service.create(1L,
                new RuleCommand("x", 5, 5, new BigDecimal("120"), FeeType.MONTHLY, true, true)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
