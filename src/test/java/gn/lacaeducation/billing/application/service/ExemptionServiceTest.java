package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.ApplicableExemption;
import gn.lacaeducation.billing.application.port.in.ExemptionUseCase.GrantExemptionCommand;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
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
                9L, ExemptionType.TOTAL, null, null, null, "Orphelin", null, null, null), 42L);

        assertThat(e.getPercentage()).isEqualByComparingTo("100");
        assertThat(e.getValidatedBy()).isEqualTo(42L);
        assertThat(e.isActive()).isTrue();
    }

    @Test
    void grant_partialWithoutPercentage_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.PARTIAL, null, null, null, "x", null, null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void grant_exceptionalWithoutAmount_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.EXCEPTIONAL, null, null, null, "geste", null, null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void grant_blankReason_throws() {
        assertThatThrownBy(() -> service.grant(1L, new GrantExemptionCommand(
                9L, ExemptionType.TOTAL, null, null, null, "  ", null, null, null), 42L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void revoke_archivesWithoutDeleting() {
        when(repo.findById(5L)).thenReturn(Optional.of(Exemption.builder().id(5L).active(true).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Exemption e = service.revoke(5L);

        assertThat(e.isActive()).isFalse();
    }

    // ── computeApplicable (§8, appelé par InvoiceService) ───────────────────────────────────────────

    private Exemption.ExemptionBuilder baseExemption() {
        return Exemption.builder().studentId(9L).active(true);
    }

    @Test
    void computeApplicable_bestPercentageAmongTotalAndPartial_wins() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                baseExemption().type(ExemptionType.PARTIAL).percentage(new BigDecimal("30")).reason("Partielle").build(),
                baseExemption().type(ExemptionType.TOTAL).percentage(new BigDecimal("100")).reason("Totale").build()));

        ApplicableExemption r = service.computeApplicable(9L, FeeType.MONTHLY);

        assertThat(r.percentage()).isEqualByComparingTo("100");
        assertThat(r.percentageReason()).isEqualTo("Totale");
    }

    @Test
    void computeApplicable_exceptionalAmounts_sumAcrossMultipleGrants() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                baseExemption().type(ExemptionType.EXCEPTIONAL).amount(new BigDecimal("10")).reason("Geste 1").build(),
                baseExemption().type(ExemptionType.EXCEPTIONAL).amount(new BigDecimal("5")).reason("Geste 2").build()));

        ApplicableExemption r = service.computeApplicable(9L, FeeType.MONTHLY);

        assertThat(r.exceptionalAmount()).isEqualByComparingTo("15");
        assertThat(r.exceptionalReason()).contains("Geste 1").contains("Geste 2");
        assertThat(r.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void computeApplicable_inactiveExemption_ignored() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                Exemption.builder().studentId(9L).active(false).type(ExemptionType.TOTAL)
                        .percentage(new BigDecimal("100")).reason("Révoquée").build()));

        ApplicableExemption r = service.computeApplicable(9L, FeeType.MONTHLY);

        assertThat(r.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void computeApplicable_wrongFeeType_ignored() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                baseExemption().type(ExemptionType.TOTAL).percentage(new BigDecimal("100"))
                        .feeTypes(List.of(FeeType.TRANSPORT)).reason("Transport seulement").build()));

        ApplicableExemption r = service.computeApplicable(9L, FeeType.MONTHLY);

        assertThat(r.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void computeApplicable_outsideDateWindow_ignored() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                baseExemption().type(ExemptionType.TOTAL).percentage(new BigDecimal("100"))
                        .startDate(LocalDate.now().plusDays(10)).reason("Pas encore active").build()));

        ApplicableExemption r = service.computeApplicable(9L, FeeType.MONTHLY);

        assertThat(r.percentage()).isEqualByComparingTo("0");
    }

    @Test
    void computeApplicable_multipleFeeTypes_appliesToEachOfThem() {
        when(repo.findByStudentId(9L)).thenReturn(List.of(
                baseExemption().type(ExemptionType.TOTAL).percentage(new BigDecimal("100"))
                        .feeTypes(List.of(FeeType.TRANSPORT, FeeType.CANTEEN)).reason("Transport + cantine").build()));

        assertThat(service.computeApplicable(9L, FeeType.TRANSPORT).percentage()).isEqualByComparingTo("100");
        assertThat(service.computeApplicable(9L, FeeType.CANTEEN).percentage()).isEqualByComparingTo("100");
        assertThat(service.computeApplicable(9L, FeeType.MONTHLY).percentage()).isEqualByComparingTo("0");
    }
}
