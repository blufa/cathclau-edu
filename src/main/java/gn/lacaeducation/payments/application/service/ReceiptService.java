package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.ReceiptRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class ReceiptService implements ReceiptUseCase {
    private final ReceiptRepository repo;
    private final PaymentRepository paymentRepo;
    @Override public Receipt generate(Long paymentId) {
        Payment p = paymentRepo.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return repo.save(Receipt.builder().paymentId(paymentId)
                .reference("REC-" + paymentId + "-" + System.currentTimeMillis())
                .amount(p.getAmount()).generatedAt(LocalDateTime.now()).build());
    }
    @Override @Transactional(readOnly=true) public Optional<Receipt> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Receipt> findByPaymentId(Long id) { return repo.findByPaymentId(id); }
    @Override @Transactional(readOnly=true) public Optional<Receipt> findByReference(String ref) { return repo.findByReference(ref); }
}
