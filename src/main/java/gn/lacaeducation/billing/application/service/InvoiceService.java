package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.event.InvoiceIssuedEvent;
import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.event.InvoicePaymentAppliedEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.*;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.RecoveryStage;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.MediaUploadService;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service @Transactional @RequiredArgsConstructor
public class InvoiceService implements InvoiceUseCase {
    private final InvoiceRepository repo;
    private final SchoolFeeRepository schoolFeeRepository;
    private final InstallmentRepository installmentRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final InvoicePdfGenerator pdfGenerator;
    private final MediaUploadService mediaUploadService;
    private final TenantInfoProvider tenantInfoProvider;
    private final gn.lacaeducation.billing.application.port.in.FamilyDiscountUseCase familyDiscountUseCase;

    @Override @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] pdfBytes(Long invoiceId) {
        Invoice inv = repo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        return pdfGenerator.generate(inv);
    }

    @Override public String pdfUrl(Long invoiceId) {
        Invoice inv = repo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        // On régénère si l'URL manque ou provient de l'ancien upload image (PDF rasterisé/vide).
        if (inv.getPdfFile() != null && !inv.getPdfFile().isBlank() && !inv.getPdfFile().contains("/image/upload/")) {
            return inv.getPdfFile();
        }
        // Génération à la demande puis stockage Cloudinary, pour ne pas régénérer à chaque appel.
        try {
            byte[] pdf = pdfGenerator.generate(inv);
            String url = mediaUploadService.uploadDocument(pdf, inv.getReference() + ".pdf", "invoices");
            inv.setPdfFile(url);
            repo.save(inv);
            log.info("PDF de la facture {} généré et stocké : {}", inv.getReference(), url);
            return url;
        } catch (RuntimeException ex) {
            log.error("Échec génération/upload du PDF de la facture {} (id={}) : {}",
                    inv.getReference(), invoiceId, ex.getMessage(), ex);
            throw new BusinessRuleException("Le PDF de la facture n'a pas pu être généré "
                    + "(vérifiez la configuration Cloudinary app.cloudinary.*)");
        }
    }

    @Override
    public Invoice create(CreateInvoiceCommand c) {
        String reference = (c.reference() == null || c.reference().isBlank())
                ? generateReference(c.tenantId()) : c.reference();
        Invoice inv = repo.save(Invoice.builder().tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                .academicYearId(c.academicYearId())
                .reference(reference).totalAmount(c.totalAmount()).paidAmount(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO)
                .remainingBalance(c.totalAmount()).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now()).recoveryStage(RecoveryStage.NONE).build());
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(c.tenantId(), c.studentId(), inv.getId(), inv.getReference()));
        eventPublisher.publishEvent(new InvoiceIssuedEvent(inv.getId(), c.studentId(), c.tenantId()));
        return inv;
    }

    @Override public List<Invoice> generateMonthly(GenerateMonthlyCommand c) {
        // Un élève non inscrit (aucune inscription en cours) ne peut pas payer la mensualité.
        // Une inscription PENDING suffit : on peut payer l'inscription + les premiers mois en même temps.
        if (!enrollmentQueryPort.hasActiveEnrollment(c.studentId())) {
            throw new BusinessRuleException("L'élève n'est pas inscrit : impossible de générer les mensualités. "
                    + "Inscrivez-le d'abord dans une classe.");
        }
        List<gn.lacaeducation.billing.domain.Installment> months = installmentRepository.findByFeeId(c.feeId());
        if (months.isEmpty()) {
            throw new BusinessRuleException("Aucune échéance (mois) configurée pour le frais id=" + c.feeId()
                    + " ; définissez les mensualités avant de facturer.");
        }
        // Réduction familiale (§5) applicable à cet élève sur les mensualités — calculée une seule fois.
        var familyDiscount = familyDiscountUseCase.computeApplicable(
                c.tenantId(), c.studentId(), gn.lacaeducation.billing.domain.FeeType.MONTHLY);
        BigDecimal discountPercent = familyDiscount.percentage() != null
                ? familyDiscount.percentage() : BigDecimal.ZERO;

        List<Invoice> created = new ArrayList<>();
        for (gn.lacaeducation.billing.domain.Installment month : months) {
            // Idempotence : on ne recrée pas la facture d'un mois déjà facturé pour cet élève.
            if (repo.existsByStudentIdAndInstallmentId(c.studentId(), month.getId())) {
                continue;
            }
            BigDecimal amount = month.getAmount();
            BigDecimal discount = amount.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal net = amount.subtract(discount);
            String reference = generateReference(c.tenantId());
            Invoice inv = repo.save(Invoice.builder()
                    .tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                    .academicYearId(c.academicYearId())
                    .reference(reference).totalAmount(amount).paidAmount(BigDecimal.ZERO).discountAmount(discount)
                    .discountReason(discount.signum() > 0 ? "Réduction familiale : " + familyDiscount.ruleLabel() : null)
                    .remainingBalance(net).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now())
                    .recoveryStage(RecoveryStage.NONE)
                    .installmentId(month.getId()).periodLabel(month.getLabel()).build());
            eventPublisher.publishEvent(new InvoiceGeneratedEvent(c.tenantId(), c.studentId(), inv.getId(), inv.getReference()));
            eventPublisher.publishEvent(new InvoiceIssuedEvent(inv.getId(), c.studentId(), c.tenantId()));
            created.add(inv);
        }
        log.info("Génération mensuelle : {} facture(s) créée(s) pour l'élève id={} frais id={}",
                created.size(), c.studentId(), c.feeId());
        return created;
    }

    /** Référence facture lisible et unique : &lt;PREFIXE_ECOLE&gt;-FAC-&lt;année&gt;-&lt;suffixe&gt;. */
    private String generateReference(Long tenantId) {
        String prefix = gn.lacaeducation.common.SchoolCode.prefix(tenantInfoProvider, tenantId);
        String year = String.valueOf(LocalDate.now().getYear());
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-FAC-" + year + "-" + suffix;
    }

    @Override public List<Invoice> generateBulk(BulkGenerateCommand cmd) {
        schoolFeeRepository.findById(cmd.feeId())
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", cmd.feeId()));

        List<Long> studentIds = enrollmentQueryPort.findValidatedStudentIdsByClassId(cmd.classId());
        List<Invoice> created = new ArrayList<>();

        for (Long studentId : studentIds) {
            if (repo.existsByStudentIdAndFeeId(studentId, cmd.feeId())) {
                log.debug("Skipping bulk invoice: already exists for student id={} fee id={}", studentId, cmd.feeId());
                continue;
            }
            String reference = "BULK-" + cmd.tenantId() + "-" + studentId + "-" + cmd.feeId()
                    + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            var fee = schoolFeeRepository.findById(cmd.feeId()).orElseThrow();
            Invoice inv = create(new CreateInvoiceCommand(cmd.tenantId(), studentId, cmd.feeId(), reference, fee.getAmount(), cmd.academicYearId()));
            created.add(inv);
        }
        log.info("Bulk invoice generation: created {} invoices for class id={} fee id={}", created.size(), cmd.classId(), cmd.feeId());
        return created;
    }

    @Override @Transactional(readOnly=true) public Optional<Invoice> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Invoice> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantIdAndStatus(Long t, InvoiceStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByTenantIdAndAcademicYearId(Long t, Long y) { return repo.findByTenantIdAndAcademicYearId(t, y); }
    @Override @Transactional(readOnly=true) public List<Invoice> findByFeeId(Long feeId) { return repo.findByFeeId(feeId); }
    @Override @Transactional(readOnly=true) public boolean existsByStudentIdAndFeeId(Long studentId, Long feeId) { return repo.existsByStudentIdAndFeeId(studentId, feeId); }
    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount) {
        Invoice inv = load(invoiceId);
        inv.setPaidAmount(inv.getPaidAmount().add(amount));
        recomputeBalance(inv);
        boolean fullySettled = inv.getStatus() == InvoiceStatus.PAID;
        Invoice saved = repo.save(inv);
        eventPublisher.publishEvent(new InvoicePaymentAppliedEvent(inv.getTenantId(), inv.getStudentId(),
                inv.getId(), inv.getReference(), amount, inv.getRemainingBalance()));
        if (fullySettled) {
            eventPublisher.publishEvent(
                    new InvoicePaidEvent(inv.getTenantId(), inv.getStudentId(), inv.getId(), inv.getReference()));
        }
        return saved;
    }

    @Override public Invoice applyDiscount(Long invoiceId, BigDecimal amount, String reason) {
        Invoice inv = load(invoiceId);
        BigDecimal d = amount != null ? amount : BigDecimal.ZERO;
        if (d.signum() < 0) throw new BusinessRuleException("La remise ne peut pas être négative");
        if (d.compareTo(inv.getTotalAmount()) > 0) {
            throw new BusinessRuleException("La remise (" + d + ") dépasse le montant total de la facture");
        }
        inv.setDiscountAmount(d);
        inv.setDiscountReason(reason);
        recomputeBalance(inv);
        return repo.save(inv);
    }

    /** Recalcule le reste à payer et le statut : net dû = total - remise ; reste = net - payé. */
    private void recomputeBalance(Invoice inv) {
        BigDecimal discount = inv.getDiscountAmount() != null ? inv.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal net = inv.getTotalAmount().subtract(discount);
        if (net.signum() < 0) net = BigDecimal.ZERO;
        BigDecimal paid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
        inv.setRemainingBalance(net.subtract(paid));
        if (inv.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) inv.setStatus(InvoiceStatus.PAID);
        else if (paid.signum() > 0) inv.setStatus(InvoiceStatus.PARTIAL);
        else inv.setStatus(InvoiceStatus.UNPAID);
    }

    @Override public Invoice advanceRecovery(Long invoiceId) {
        Invoice inv = load(invoiceId);
        RecoveryStage current = inv.getRecoveryStage() == null
                ? RecoveryStage.NONE : inv.getRecoveryStage();
        RecoveryStage next = switch (current) {
            case NONE -> RecoveryStage.REMINDER_1;
            case REMINDER_1 -> RecoveryStage.REMINDER_2;
            case REMINDER_2 -> RecoveryStage.EXCLUDED;
            case EXCLUDED -> RecoveryStage.EXCLUDED;
        };
        inv.setRecoveryStage(next);
        inv.setLastReminderAt(LocalDate.now());
        return repo.save(inv);
    }

    @Override public Invoice setRecoveryStage(Long invoiceId, gn.lacaeducation.billing.domain.RecoveryStage stage) {
        Invoice inv = load(invoiceId);
        inv.setRecoveryStage(stage != null ? stage : gn.lacaeducation.billing.domain.RecoveryStage.NONE);
        inv.setLastReminderAt(LocalDate.now());
        return repo.save(inv);
    }

    @Override public void cancel(Long id) { repo.deleteById(id); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    @Override @Transactional(readOnly=true) public boolean existsById(Long id) { return repo.existsById(id); }
    private Invoice load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id)); }
}
