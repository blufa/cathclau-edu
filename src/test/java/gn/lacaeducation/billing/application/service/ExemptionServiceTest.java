package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.GrantExemptionCommand;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.common.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExemptionServiceTest {

    @Mock ExemptionRepository repo;
    @InjectMocks ExemptionService service;

    @Test
    void grant_total_forces100PercentAndTracksValidator() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Exemption e = service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.TOTAL, null, null, null, "Orphelin", null, null), 42L);

        assertThat(e.getPercentage()).isEqualByComparingTo("100");
        assertThat(e.getValidatedBy()).isEqualTo(42L);
        assertThat(e.isActive()).isTrue();
    }

    @Test
    void grant_partialWithoutPercentage_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.PARTIAL, null, null, null, "x", null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void grant_exceptionalWithoutAmount_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.EXCEPTIONAL, null, null, null, "geste", null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void grant_blankReason_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.TOTAL, null, null, null, "  ", null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void revoke_archivesWithoutDeleting() {
        when(repo.findById(5L)).thenReturn(Optional.of(Exemption.builder().id(5L).active(true).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Exemption e = service.revoke(5L);

        assertThat(e.isActive()).isFalse();
    }
}
