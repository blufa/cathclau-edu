package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.ReceiptRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import gn.lacaeducation.payments.domain.Receipt;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock ReceiptRepository repo;
    @Mock PaymentRepository paymentRepo;
    @InjectMocks ReceiptService service;

    @Test
    void generate_forSucceededPayment_createsReceiptWithPdfUrl() {
        when(repo.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(Payment.builder()
                .id(1L).tenantId(10L).amount(new BigDecimal("100.00")).status(PaymentStatus.SUCCEEDED).build()));
        when(repo.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        Receipt r = service.generate(1L);

        assertThat(r.getReference()).startsWith("REC-10-");
        assertThat(r.getPdfFile()).contains("/files/receipts/");
        assertThat(r.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void generate_isIdempotent_returnsExisting() {
        Receipt existing = Receipt.builder().id(5L).paymentId(1L).reference("REC-OLD").build();
        when(repo.findByPaymentId(1L)).thenReturn(Optional.of(existing));

        Receipt r = service.generate(1L);

        assertThat(r).isSameAs(existing);
        verify(repo, never()).save(any());
    }

    @Test
    void generate_rejectsNonSucceededPayment() {
        when(repo.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).build()));

        assertThatThrownBy(() -> service.generate(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generate_unknownPayment_throwsNotFound() {
        when(repo.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(paymentRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStudentId_collectsReceiptsAcrossStudentPayments() {
        when(paymentRepo.findByStudentId(7L)).thenReturn(List.of(
                Payment.builder().id(1L).build(), Payment.builder().id(2L).build()));
        when(repo.findByPaymentId(1L)).thenReturn(Optional.of(Receipt.builder().id(11L).paymentId(1L).build()));
        when(repo.findByPaymentId(2L)).thenReturn(Optional.empty());

        List<Receipt> receipts = service.findByStudentId(7L);

        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).getPaymentId()).isEqualTo(1L);
    }
}
