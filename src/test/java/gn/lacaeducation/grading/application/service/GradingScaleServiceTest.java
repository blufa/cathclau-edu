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
                BigDecimal.valueOf(16), BigDecimal.valueOf(18), null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_customScale_appliesToHonor() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        GradingScale saved = service.update(10L, new UpdateScaleCommand(
                BigDecimal.valueOf(12), BigDecimal.valueOf(12), BigDecimal.valueOf(15),
                BigDecimal.valueOf(17), BigDecimal.valueOf(19), null, null, null));

        // With this stricter scale, 16/20 is only "Assez Bien" and 11/20 fails the pass threshold.
        assertThat(saved.honorFor(BigDecimal.valueOf(16))).isEqualTo("Assez Bien");
        assertThat(saved.decisionFor(BigDecimal.valueOf(11))).isEqualTo("REDOUBLE");
        // Distinction thresholds left null → default barème (Félicitations ≥ 16, Compliments ≥ 13).
        assertThat(saved.getFelicitationsMin()).isEqualByComparingTo("16");
        assertThat(saved.getComplimentsMin()).isEqualByComparingTo("13");
    }

    @Test
    void update_customDistinctionThresholds_applied() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        GradingScale saved = service.update(10L, new UpdateScaleCommand(
                BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(14),
                BigDecimal.valueOf(16), BigDecimal.valueOf(18),
                BigDecimal.valueOf(17), BigDecimal.valueOf(14), BigDecimal.valueOf(9)));

        // Félicitations à 17, Compliments à 14, aucune matière < 9.
        assertThat(saved.distinctionFor(BigDecimal.valueOf(17), BigDecimal.valueOf(9)))
                .isEqualTo(gn.lacaeducation.grading.domain.Distinction.FELICITATIONS);
        assertThat(saved.distinctionFor(BigDecimal.valueOf(15), BigDecimal.valueOf(10)))
                .isEqualTo(gn.lacaeducation.grading.domain.Distinction.COMPLIMENTS);
        // Une matière sous le seuil (8 < 9) annule la distinction malgré une bonne moyenne.
        assertThat(saved.distinctionFor(BigDecimal.valueOf(18), BigDecimal.valueOf(8)))
                .isEqualTo(gn.lacaeducation.grading.domain.Distinction.NONE);
    }

    @Test
    void update_complimentsAboveFelicitations_throws() {
        assertThatThrownBy(() -> service.update(10L, new UpdateScaleCommand(
                BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(14),
                BigDecimal.valueOf(16), BigDecimal.valueOf(18),
                BigDecimal.valueOf(14), BigDecimal.valueOf(15), BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
