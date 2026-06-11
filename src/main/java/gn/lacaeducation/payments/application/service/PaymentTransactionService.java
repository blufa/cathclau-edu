package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.payments.application.port.in.PaymentTransactionUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class PaymentTransactionService implements PaymentTransactionUseCase {
    private final PaymentTransactionRepository repo;
    @Override public PaymentTransaction record(RecordTransactionCommand c) {
        return repo.save(PaymentTransaction.builder().paymentId(c.paymentId()).provider(c.provider())
                .externalTransactionId(c.externalTransactionId()).providerStatus(c.providerStatus())
                .responsePayload(c.responsePayload()).createdAt(LocalDateTime.now()).build());
    }
    @Override @Transactional(readOnly=true) public Optional<PaymentTransaction> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<PaymentTransaction> findByPaymentId(Long id) { return repo.findByPaymentId(id); }
    @Override @Transactional(readOnly=true) public Optional<PaymentTransaction> findByExternalId(String ext) { return repo.findByExternalTransactionId(ext); }
}
