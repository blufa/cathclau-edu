package gn.lacaeducation.payments.adapters.out;

import gn.lacaeducation.payments.StudentReceiptProvider;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentReceiptProviderAdapter implements StudentReceiptProvider {

    private final ReceiptUseCase receiptUseCase;
    private final PaymentUseCase paymentUseCase;

    @Override
    public List<ReceiptInfo> listReceipts(Long studentId) {
        return receiptUseCase.findByStudentId(studentId).stream()
                .map(r -> new ReceiptInfo(r.getId(), r.getReference(), r.getAmount(), r.getGeneratedAt()))
                .toList();
    }

    @Override
    public Optional<Long> receiptStudentId(Long receiptId) {
        return receiptUseCase.findById(receiptId)
                .flatMap(r -> paymentUseCase.findById(r.getPaymentId()))
                .map(Payment::getStudentId);
    }

    @Override
    public byte[] receiptPdfBytes(Long receiptId) {
        return receiptUseCase.pdfBytes(receiptId);
    }
}
