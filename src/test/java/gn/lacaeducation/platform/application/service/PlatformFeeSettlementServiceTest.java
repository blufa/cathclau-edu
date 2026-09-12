package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.payments.DjomyGatewayApi;
import gn.lacaeducation.platform.application.port.in.PlatformFeeSettlementUseCase.TenantBalance;
import gn.lacaeducation.platform.application.port.out.PlatformFeeLedgerRepository;
import gn.lacaeducation.platform.application.port.out.PlatformFeeSettlementRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Solde des frais plateforme (§14) : registre encaissé − règlements réussis, jamais négatif. */
@ExtendWith(MockitoExtension.class)
class PlatformFeeSettlementServiceTest {

    @Mock PlatformFeeLedgerRepository ledgerRepository;
    @Mock PlatformFeeSettlementRepository settlementRepository;
    @Mock DjomyGatewayApi djomyGatewayApi;
    @Mock TenantRepository tenantRepository;
    @InjectMocks PlatformFeeSettlementService service;

    @Test
    void currentBalance_isCollectedMinusSettled() {
        when(ledgerRepository.sumForCurrentTenant()).thenReturn(new BigDecimal("50000"));
        when(settlementRepository.sumSucceededForCurrentTenant()).thenReturn(new BigDecimal("20000"));

        assertThat(service.currentBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void currentBalance_neverNegative_whenSettledExceedsCollected() {
        when(ledgerRepository.sumForCurrentTenant()).thenReturn(new BigDecimal("10000"));
        when(settlementRepository.sumSucceededForCurrentTenant()).thenReturn(new BigDecimal("15000"));

        assertThat(service.currentBalance()).isEqualByComparingTo("0");
    }

    @Test
    void initiate_withNoBalanceDue_throwsBusinessRule() {
        when(ledgerRepository.sumForCurrentTenant()).thenReturn(BigDecimal.ZERO);
        when(settlementRepository.sumSucceededForCurrentTenant()).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.initiate("620000000")).isInstanceOf(BusinessRuleException.class);
    }

    /** Vue SUPER_ADMIN : une école par ligne, y compris celles sans rien dû (solde 0), triées par
     * solde décroissant — pour repérer en un coup d'œil qui doit le plus à la plateforme. */
    @Test
    void balancesByTenant_listsEverySchool_sortedByBalanceDescending() {
        when(tenantRepository.findAll()).thenReturn(List.of(
                Tenant.builder().id(1L).schoolName("École A").build(),
                Tenant.builder().id(2L).schoolName("École B").build()));
        when(ledgerRepository.sumByTenantId(1L)).thenReturn(new BigDecimal("10000"));
        when(settlementRepository.sumSucceededByTenantId(1L)).thenReturn(BigDecimal.ZERO);
        when(ledgerRepository.sumByTenantId(2L)).thenReturn(new BigDecimal("50000"));
        when(settlementRepository.sumSucceededByTenantId(2L)).thenReturn(new BigDecimal("10000"));

        List<TenantBalance> balances = service.balancesByTenant();

        assertThat(balances).hasSize(2);
        assertThat(balances.get(0).tenantName()).isEqualTo("École B");
        assertThat(balances.get(0).balance()).isEqualByComparingTo("40000");
        assertThat(balances.get(1).tenantName()).isEqualTo("École A");
        assertThat(balances.get(1).balance()).isEqualByComparingTo("10000");
    }

    @Test
    void balancesByTenant_includesSchoolsWithZeroBalance() {
        when(tenantRepository.findAll()).thenReturn(List.of(Tenant.builder().id(3L).schoolName("École C").build()));
        when(ledgerRepository.sumByTenantId(3L)).thenReturn(BigDecimal.ZERO);
        when(settlementRepository.sumSucceededByTenantId(3L)).thenReturn(BigDecimal.ZERO);

        List<TenantBalance> balances = service.balancesByTenant();

        assertThat(balances).hasSize(1);
        assertThat(balances.getFirst().balance()).isEqualByComparingTo("0");
    }
}
