package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.common.MediaUploadService;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.ReceiptPdfGenerator;
import gn.lacaeducation.payments.application.port.out.ReceiptRepository;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import gn.lacaeducation.payments.domain.Receipt;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service @Transactional @RequiredArgsConstructor
public class ReceiptService implements ReceiptUseCase {
    private final ReceiptRepository repo;
    private final PaymentRepository paymentRepo;
    private final ReceiptPdfGenerator pdfGenerator;
    private final MediaUploadService mediaUploadService;
    private final TenantInfoProvider tenantInfoProvider;
    private final PayerDirectory payerDirectory;

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
        Receipt receipt = repo.save(Receipt.builder().paymentId(paymentId)
                .reference(reference).amount(p.getAmount())
                .studentName(resolveStudentName(p.getStudentId()))
                .collectedByName(resolveCollectedByName(p.getCollectedByUserId()))
                .generatedAt(LocalDateTime.now()).build());
        // Génération du PDF + upload Cloudinary, sans bloquer le paiement si l'upload échoue.
        String url = renderAndUpload(receipt, p);
        if (url != null) {
            receipt.setPdfFile(url);
            receipt = repo.save(receipt);
        }
        return receipt;
    }

    @Override public Receipt generateGrouped(Long paymentId, String coveredMonths, Integer monthsCount,
                                             java.math.BigDecimal remainingAfter) {
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
        Receipt receipt = repo.save(Receipt.builder().paymentId(paymentId)
                .reference(reference).amount(p.getAmount())
                .studentName(resolveStudentName(p.getStudentId()))
                .collectedByName(resolveCollectedByName(p.getCollectedByUserId()))
                .coveredMonths(coveredMonths).monthsCount(monthsCount).remainingAfter(remainingAfter)
                .generatedAt(LocalDateTime.now()).build());
        String url = renderAndUpload(receipt, p);
        if (url != null) {
            receipt.setPdfFile(url);
            receipt = repo.save(receipt);
        }
        return receipt;
    }

    @Override @Transactional(readOnly = true)
    public byte[] pdfBytes(Long receiptId) {
        Receipt receipt = repo.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));
        Payment p = paymentRepo.findById(receipt.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", receipt.getPaymentId()));
        return pdfGenerator.generate(receipt, p);
    }

    @Override public String pdfUrl(Long receiptId) {
        Receipt receipt = repo.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));
        if (isUsablePdf(receipt.getPdfFile())) {
            return receipt.getPdfFile();
        }
        // PDF pas encore disponible (ex. upload échoué à la création) : on tente de le (re)générer.
        Payment p = paymentRepo.findById(receipt.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", receipt.getPaymentId()));
        String url = renderAndUpload(receipt, p);
        if (url == null) {
            throw new BusinessRuleException("Le PDF du reçu n'a pas pu être généré "
                    + "(vérifiez la configuration Cloudinary app.cloudinary.*)");
        }
        receipt.setPdfFile(url);
        repo.save(receipt);
        return url;
    }

    /** Une URL est exploitable si présente et pas issue de l'ancien upload image (PDF rasterisé/vide). */
    private static boolean isUsablePdf(String url) {
        return url != null && !url.isBlank() && !url.contains("/image/upload/");
    }

    /** Snapshot du nom de l'élève au moment de l'émission ; ne bloque jamais la génération du reçu. */
    private String resolveStudentName(Long studentId) {
        if (studentId == null) return null;
        try {
            return payerDirectory.studentName(studentId).orElse(null);
        } catch (RuntimeException e) {
            log.warn("Nom d'élève non résolu pour le reçu (élève {}) : {}", studentId, e.getMessage());
            return null;
        }
    }

    /** Snapshot du nom de l'agent ayant encaissé (« Opérateur ») ; ne bloque jamais la génération du reçu. */
    private String resolveCollectedByName(Long userId) {
        if (userId == null) return null;
        try {
            return payerDirectory.operatorName(userId).orElse(null);
        } catch (RuntimeException e) {
            log.warn("Nom de l'opérateur non résolu pour le reçu (utilisateur {}) : {}", userId, e.getMessage());
            return null;
        }
    }

    private String renderAndUpload(Receipt receipt, Payment payment) {
        try {
            byte[] pdf = pdfGenerator.generate(receipt, payment);
            String url = mediaUploadService.uploadDocument(pdf, receipt.getReference() + ".pdf", "receipts");
            log.info("PDF du reçu {} généré et stocké : {}", receipt.getReference(), url);
            return url;
        } catch (RuntimeException ex) {
            log.error("Échec génération/upload du PDF du reçu {} (paiement id={}) : {}",
                    receipt.getReference(), payment.getId(), ex.getMessage(), ex);
            return null;
        }
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
        String prefix = SchoolCode.prefix(tenantInfoProvider, tenantId);
        String year = String.valueOf(LocalDate.now().getYear());
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-REC-" + year + "-" + suffix;
    }
}
