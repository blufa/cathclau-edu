package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.BusinessRuleException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolFeeServiceTest {

    @Mock SchoolFeeRepository repo;
    @Mock InstallmentRepository installmentRepo;
    @Mock InvoiceRepository invoiceRepo;
    @InjectMocks SchoolFeeService service;

    private final SchoolFee fee = SchoolFee.builder().id(168L).tenantId(1L)
            .type(FeeType.MONTHLY).amount(new BigDecimal("500000")).academicYear("2026-2027").build();

    @Test
    void delete_noInvoices_cascadesInstallmentsThenDeletesFee() {
        when(repo.findById(168L)).thenReturn(Optional.of(fee));
        when(invoiceRepo.findByFeeId(168L)).thenReturn(List.of());
        Installment i1 = Installment.builder().id(1L).feeId(168L).build();
        Installment i2 = Installment.builder().id(2L).feeId(168L).build();
        when(installmentRepo.findByFeeId(168L)).thenReturn(List.of(i1, i2));

        service.delete(168L);

        verify(installmentRepo).deleteById(1L);
        verify(installmentRepo).deleteById(2L);
        verify(repo).deleteById(168L);
    }

    @Test
    void delete_feeAlreadyInvoiced_throwsBusinessRuleException_andDeletesNothing() {
        when(repo.findById(168L)).thenReturn(Optional.of(fee));
        when(invoiceRepo.findByFeeId(168L)).thenReturn(List.of(Invoice.builder().id(9L).feeId(168L).build()));

        assertThatThrownBy(() -> service.delete(168L)).isInstanceOf(BusinessRuleException.class);

        verify(installmentRepo, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
        verify(repo, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
    }
}
