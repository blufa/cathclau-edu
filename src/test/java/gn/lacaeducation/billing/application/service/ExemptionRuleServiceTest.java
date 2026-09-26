package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionRuleUseCase.RuleCommand;
import gn.lacaeducation.billing.application.port.out.ExemptionRuleRepository;
import gn.lacaeducation.billing.domain.ExemptionRule;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExemptionRuleServiceTest {

    @Mock ExemptionRuleRepository repo;
    @InjectMocks ExemptionRuleService service;

    @Test
    void create_savesRule_defaultsActiveToTrue() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ExemptionRule r = service.create(1L, new RuleCommand(
                "Orphelin", ExemptionType.TOTAL, null, null, null, null, null));

        assertThat(r.getTenantId()).isEqualTo(1L);
        assertThat(r.getLabel()).isEqualTo("Orphelin");
        assertThat(r.isActive()).isTrue();
    }

    @Test
    void create_withoutLabel_throws() {
        assertThatThrownBy(() -> service.create(1L, new RuleCommand(
                "", ExemptionType.TOTAL, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_withoutType_throws() {
        assertThatThrownBy(() -> service.create(1L, new RuleCommand(
                "Orphelin", null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_modifiesExistingRule() {
        ExemptionRule existing = ExemptionRule.builder().id(5L).tenantId(1L).label("Ancien")
                .type(ExemptionType.PARTIAL).percentage(BigDecimal.TEN).active(true).build();
        when(repo.findById(5L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ExemptionRule updated = service.update(5L, new RuleCommand(
                "Réduction sociale", ExemptionType.PARTIAL, BigDecimal.valueOf(50), null,
                List.of(FeeType.MONTHLY), false, false));

        assertThat(updated.getLabel()).isEqualTo("Réduction sociale");
        assertThat(updated.getPercentage()).isEqualByComparingTo("50");
        assertThat(updated.getFeeTypes()).containsExactly(FeeType.MONTHLY);
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void update_unknownRule_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new RuleCommand(
                "X", ExemptionType.TOTAL, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_delegatesToRepository() {
        service.delete(7L);
        org.mockito.Mockito.verify(repo).deleteById(7L);
    }
}
