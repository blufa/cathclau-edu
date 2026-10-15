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
    @Mock gn.lacaeducation.payments.application.port.out.ReceiptPdfGenerator pdfGenerator;
    @Mock gn.lacaeducation.common.MediaUploadService mediaUploadService;
    @Mock gn.lacaeducation.common.TenantInfoProvider tenantInfoProvider;
    @Mock gn.lacaeducation.payments.PayerDirectory payerDirectory;
    @InjectMocks ReceiptService service;

    @Test
    void generate_forSucceededPayment_createsReceiptWithoutRenderingPdfYet() {
        // Le PDF (rendu + upload Cloudinary, potentiellement lent) ne doit PAS être généré ici — ça
        // bloquerait l'encaissement le temps de l'appel réseau. Génération paresseuse via pdfUrl().
        when(repo.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(Payment.builder()
                .id(1L).tenantId(10L).amount(new BigDecimal("100.00")).status(PaymentStatus.SUCCEEDED).build()));
        when(repo.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        Receipt r = service.generate(1L);

        // Référence au format <codeÉcole>-REC-<année>-<identifiant>, ex. EC-REC-2026-046E7C.
        assertThat(r.getReference()).matches("[A-Z]{2}-REC-\\d{4}-.*");
        assertThat(r.getPdfFile()).isNull();
        assertThat(r.getAmount()).isEqualByComparingTo("100.00");
        verifyNoInteractions(pdfGenerator, mediaUploadService);
    }

    @Test
    void pdfUrl_notYetGenerated_rendersUploadsAndCaches() {
        Receipt receipt = Receipt.builder().id(1L).paymentId(1L).reference("EC-REC-2026-AAA").pdfFile(null).build();
        when(repo.findById(1L)).thenReturn(Optional.of(receipt));
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(Payment.builder().id(1L).status(PaymentStatus.SUCCEEDED).build()));
        when(pdfGenerator.generate(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(mediaUploadService.uploadDocument(any(), any(), any())).thenReturn("https://cdn.test/raw/receipts/rec.pdf");
        when(repo.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        String url = service.pdfUrl(1L);

        assertThat(url).isEqualTo("https://cdn.test/raw/receipts/rec.pdf");
        assertThat(receipt.getPdfFile()).isEqualTo("https://cdn.test/raw/receipts/rec.pdf");
    }

    @Test
    void pdfUrl_alreadyCached_returnsWithoutRegenerating() {
        Receipt receipt = Receipt.builder().id(1L).paymentId(1L).pdfFile("https://cdn.test/raw/receipts/rec.pdf").build();
        when(repo.findById(1L)).thenReturn(Optional.of(receipt));

        String url = service.pdfUrl(1L);

        assertThat(url).isEqualTo("https://cdn.test/raw/receipts/rec.pdf");
        verifyNoInteractions(pdfGenerator, mediaUploadService);
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
    void refreshStudentName_reresolvesNameAndInvalidatesCachedPdf() {
        Receipt receipt = Receipt.builder().id(1L).paymentId(2L)
                .studentName("Awa Diallo (ancien nom mal saisi)").pdfFile("https://cdn.test/raw/receipts/stale.pdf").build();
        when(repo.findById(1L)).thenReturn(Optional.of(receipt));
        when(paymentRepo.findById(2L)).thenReturn(Optional.of(Payment.builder().id(2L).studentId(7L).build()));
        when(payerDirectory.studentName(7L)).thenReturn(Optional.of("Awa Diallo (corrigé)"));
        when(repo.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        Receipt result = service.refreshStudentName(1L);

        assertThat(result.getStudentName()).isEqualTo("Awa Diallo (corrigé)");
        assertThat(result.getPdfFile()).isNull();
    }

    @Test
    void refreshStudentName_unknownReceipt_throwsNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshStudentName(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStudentId_collectsReceiptsAcrossStudentPayments() {
        when(paymentRepo.findByStudentId(7L)).thenReturn(List.of(
                Payment.builder().id(1L).build(), Payment.builder().id(2L).build()));
        when(repo.findByPaymentId(1L)).thenReturn(Optional.of(Receipt.builder().id(11L).paymentId(1L).build()));
        when(repo.findByPaymentId(2L)).thenReturn(Optional.empty());

        List<Receipt> receipts = service.findByStudentId(7L);

        assertThat(receipts).hasSize(1);
        assertThat(receipts.getFirst().getPaymentId()).isEqualTo(1L);
    }
}
