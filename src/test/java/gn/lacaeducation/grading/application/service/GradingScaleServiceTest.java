package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.application.port.in.GradingScaleUseCase.UpdateScaleCommand;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.domain.GradingScale;
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
class GradingScaleServiceTest {

    @Mock GradingScaleRepository repo;
    @InjectMocks GradingScaleService service;

    @Test
    void getForTenant_returnsDefaultsWhenUnset() {
        when(repo.findByTenantId(10L)).thenReturn(Optional.empty());
        GradingScale scale = service.getForTenant(10L);
        assertThat(scale.getPassThreshold()).isEqualByComparingTo("10");
        assertThat(scale.honorFor(BigDecimal.valueOf(17))).isEqualTo("Bien");
    }

    @Test
    void update_nonIncreasingBands_throws() {
        assertThatThrownBy(() -> service.update(10L, new UpdateScaleCommand(
                BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(10),
                BigDecimal.valueOf(16), BigDecimal.valueOf(18))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_customScale_appliesToHonor() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        GradingScale saved = service.update(10L, new UpdateScaleCommand(
                BigDecimal.valueOf(12), BigDecimal.valueOf(12), BigDecimal.valueOf(15),
                BigDecimal.valueOf(17), BigDecimal.valueOf(19)));

        // With this stricter scale, 16/20 is only "Assez Bien" and 11/20 fails the pass threshold.
        assertThat(saved.honorFor(BigDecimal.valueOf(16))).isEqualTo("Assez Bien");
        assertThat(saved.decisionFor(BigDecimal.valueOf(11))).isEqualTo("REDOUBLE");
    }
}
