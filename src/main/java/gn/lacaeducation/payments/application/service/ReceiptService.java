package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.ReceiptRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import gn.lacaeducation.payments.domain.Receipt;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Service @Transactional @RequiredArgsConstructor
public class ReceiptService implements ReceiptUseCase {
    private final ReceiptRepository repo;
    private final PaymentRepository paymentRepo;

    @Override public Receipt generate(Long paymentId) {
        // 1-1 with payment, generated only for a SUCCEEDED payment, idempotent (§9.3).
        Optional<Receipt> existing = repo.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (p.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleException("Receipt can only be generated for a SUCCEEDED payment (id=" + paymentId + ")");
        }
        String reference = generateReference(p.getTenantId());
        // PDF URL persisted on the entity (rendering delegated to the documents pipeline, §14).
        return repo.save(Receipt.builder().paymentId(paymentId)
                .reference(reference).amount(p.getAmount())
                .pdfFile("/files/receipts/" + reference + ".pdf")
                .generatedAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly=true) public Optional<Receipt> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Receipt> findByPaymentId(Long id) { return repo.findByPaymentId(id); }
    @Override @Transactional(readOnly=true) public Optional<Receipt> findByReference(String ref) { return repo.findByReference(ref); }

    @Override @Transactional(readOnly=true) public List<Receipt> findByStudentId(Long studentId) {
        return paymentRepo.findByStudentId(studentId).stream()
                .map(p -> repo.findByPaymentId(p.getId()))
                .filter(Optional::isPresent).map(Optional::get).toList();
    }

    private String generateReference(Long tenantId) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "REC-" + tenantId + "-" + date + "-" + suffix;
    }
}
