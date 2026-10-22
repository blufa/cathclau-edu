package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.payments.application.event.CashDepositCreatedEvent;
import gn.lacaeducation.payments.application.event.CashDepositDecidedEvent;
import gn.lacaeducation.payments.application.port.in.CashDepositUseCase.CreateCashDepositCommand;
import gn.lacaeducation.payments.application.port.out.CashDepositRepository;
import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashDepositServiceTest {

    @Mock CashDepositRepository repo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CashDepositService service;

    @AfterEach void clearTenant() { TenantContext.clear(); }

    private CreateCashDepositCommand cmd(BigDecimal amount, PaymentChannel method, String idempotencyKey) {
        return new CreateCashDepositCommand(10L, 42L, amount, "GNF", method, null,
                LocalDateTime.now(), "REF-1", "commentaire", idempotencyKey);
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.create(cmd(BigDecimal.ZERO, PaymentChannel.CASH, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_rejectsNonDeskManualMethod() {
        assertThatThrownBy(() -> service.create(cmd(BigDecimal.TEN, PaymentChannel.DJOMY, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_savesPendingAndPublishesCreatedEvent() {
        when(repo.save(any())).thenAnswer(i -> { CashDeposit d = i.getArgument(0); d.setId(1L); return d; });

        CashDeposit result = service.create(cmd(new BigDecimal("500000"), PaymentChannel.ORANGE_MONEY, null));

        assertThat(result.getStatus()).isEqualTo(CashDepositStatus.PENDING);
        ArgumentCaptor<CashDepositCreatedEvent> captor = ArgumentCaptor.forClass(CashDepositCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().method()).isEqualTo("ORANGE_MONEY");
        assertThat(captor.getValue().amount()).isEqualTo(new BigDecimal("500000"));
    }

    @Test
    void create_withExistingIdempotencyKey_returnsExistingWithoutSavingOrPublishing() {
        CashDeposit existing = CashDeposit.builder().id(9L).tenantId(10L).status(CashDepositStatus.PENDING).build();
        when(repo.findByTenantIdAndIdempotencyKey(10L, "KEY-1")).thenReturn(Optional.of(existing));

        CashDeposit result = service.create(cmd(new BigDecimal("500000"), PaymentChannel.CASH, "KEY-1"));

        assertThat(result.getId()).isEqualTo(9L);
        verify(repo, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void validate_pendingDeposit_setsValidatedAndPublishesDecidedEvent() {
        CashDeposit pending = CashDeposit.builder().id(1L).tenantId(10L).accountantId(42L)
                .amount(new BigDecimal("300000")).currency("GNF").method(PaymentChannel.CASH)
                .status(CashDepositStatus.PENDING).build();
        when(repo.lockById(1L)).thenReturn(Optional.of(pending));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        CashDeposit result = service.validate(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(CashDepositStatus.VALIDATED);
        assertThat(result.getValidatedBy()).isEqualTo(99L);
        assertThat(result.getValidatedAt()).isNotNull();
        ArgumentCaptor<CashDepositDecidedEvent> captor = ArgumentCaptor.forClass(CashDepositDecidedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("VALIDATED");
        assertThat(captor.getValue().decidedBy()).isEqualTo(99L);
    }

    @Test
    void reject_pendingDeposit_setsRejectedWithReason() {
        CashDeposit pending = CashDeposit.builder().id(1L).tenantId(10L).accountantId(42L)
                .amount(new BigDecimal("300000")).currency("GNF").method(PaymentChannel.CASH)
                .status(CashDepositStatus.PENDING).build();
        when(repo.lockById(1L)).thenReturn(Optional.of(pending));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        CashDeposit result = service.reject(1L, 99L, "Montant incorrect");

        assertThat(result.getStatus()).isEqualTo(CashDepositStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Montant incorrect");
    }

    @Test
    void validate_alreadyDecidedDeposit_throwsBusinessRuleAndNeverPublishes() {
        CashDeposit validated = CashDeposit.builder().id(1L).tenantId(10L).status(CashDepositStatus.VALIDATED).build();
        when(repo.lockById(1L)).thenReturn(Optional.of(validated));

        assertThatThrownBy(() -> service.validate(1L, 99L)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void validate_depositFromAnotherTenant_throwsNotFound() {
        TenantContext.set(10L);
        CashDeposit otherTenant = CashDeposit.builder().id(1L).tenantId(999L).status(CashDepositStatus.PENDING).build();
        when(repo.lockById(1L)).thenReturn(Optional.of(otherTenant));

        assertThatThrownBy(() -> service.validate(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).save(any());
    }
}
