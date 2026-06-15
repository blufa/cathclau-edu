package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InstallmentUseCase.CreateInstallmentCommand;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
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
class InstallmentServiceTest {

    @Mock InstallmentRepository repo;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @InjectMocks InstallmentService service;

    private void feeOf(BigDecimal amount) {
        when(schoolFeeRepository.findById(1L)).thenReturn(Optional.of(
                SchoolFee.builder().id(1L).amount(amount).build()));
    }

    @Test
    void create_withinFeeTotal_savesPendingInstallment() {
        feeOf(new BigDecimal("100.00"));
        when(repo.findByFeeId(1L)).thenReturn(List.of(
                Installment.builder().id(5L).amount(new BigDecimal("40.00")).build()));
        when(repo.save(any(Installment.class))).thenAnswer(i -> i.getArgument(0));

        Installment created = service.create(new CreateInstallmentCommand(
                1L, "Tranche 2", new BigDecimal("60.00"), LocalDate.now().plusDays(30)));

        assertThat(created.getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(created.getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void create_exceedingFeeTotal_throwsBusinessRule() {
        feeOf(new BigDecimal("100.00"));
        when(repo.findByFeeId(1L)).thenReturn(List.of(
                Installment.builder().id(5L).amount(new BigDecimal("80.00")).build()));

        assertThatThrownBy(() -> service.create(new CreateInstallmentCommand(
                1L, "Tranche 2", new BigDecimal("30.00"), LocalDate.now())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_unknownFee_throwsNotFound() {
        when(schoolFeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateInstallmentCommand(
                1L, "Tranche", BigDecimal.TEN, LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
