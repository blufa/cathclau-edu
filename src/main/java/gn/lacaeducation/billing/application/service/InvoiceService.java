package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.event.InvoiceIssuedEvent;
import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.event.InvoicePaymentAppliedEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.*;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.RecoveryStage;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.MediaUploadService;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.platform.PlatformFeeLedgerApi;
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
    private final gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase staffChildBenefitUseCase;
    private final gn.lacaeducation.billing.application.port.in.ExemptionUseCase exemptionUseCase;
    private final gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase earlyPaymentDiscountUseCase;
    private final PlatformFeeLedgerApi platformFeeLedgerApi;

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

    /** Résout l'année scolaire à figer sur la facture : celle fournie par l'appelant si présente,
     * sinon celle de l'inscription active la plus récente de l'élève (jamais {@code null} en pratique
     * une fois l'élève inscrit — voir {@link EnrollmentQueryPort#findActiveAcademicYearId}). Sans ce
     * filet, une facture générée sans année scolaire explicite (ex. mensualités depuis la fiche élève,
     * factures de transport auto-générées) restait `academic_year_id = NULL` et disparaissait des
     * tableaux de bord qui filtrent strictement par année (Vision Fondateur notamment). */
    private Long resolveAcademicYearId(Long provided, Long studentId) {
        return provided != null ? provided : enrollmentQueryPort.findActiveAcademicYearId(studentId);
    }

    /**
     * Remise automatique applicable à une facture — trois sources possibles : réduction familiale
     * (fratrie, §5), avantage personnel (enfant de membre du personnel, §6), exonération TOTAL/PARTIAL
     * (§8, décision manuelle de la direction). Les trois sont calculées, mais jamais cumulées entre
     * elles : la plus avantageuse l'emporte (jamais leur somme), pour éviter qu'un cumul mécanique ne
     * produise une remise déraisonnable. Une exonération EXCEPTIONAL (montant fixe, décision ponctuelle
     * documentée), elle, s'ajoute EN PLUS du pourcentage gagnant — c'est un geste distinct, pas une
     * règle automatique concurrente des trois autres. {@code feeType == null} (fee introuvable/non
     * précisé) ne déclenche aucune remise, aucune des règles ne pouvant alors être évaluée avec
     * certitude.
     */
    private record ApplicableInvoiceDiscount(BigDecimal percentage, String percentageReason,
                                              gn.lacaeducation.billing.domain.DiscountSource source,
                                              BigDecimal exceptionalAmount, String exceptionalReason) {}

    private ApplicableInvoiceDiscount computeApplicableDiscount(Long tenantId, Long studentId, FeeType feeType) {
        if (feeType == null) return new ApplicableInvoiceDiscount(BigDecimal.ZERO, null, null, BigDecimal.ZERO, null);
        var family = familyDiscountUseCase.computeApplicable(tenantId, studentId, feeType);
        var staff = staffChildBenefitUseCase.computeCoverage(tenantId, studentId, feeType);
        var exemption = exemptionUseCase.computeApplicable(studentId, feeType);

        BigDecimal familyPercent = family.percentage() != null ? family.percentage() : BigDecimal.ZERO;
        BigDecimal staffPercent = staff.coveragePercent() != null ? staff.coveragePercent() : BigDecimal.ZERO;
        BigDecimal exemptionPercent = exemption.percentage() != null ? exemption.percentage() : BigDecimal.ZERO;

        BigDecimal bestPercent = BigDecimal.ZERO;
        String bestReason = null;
        gn.lacaeducation.billing.domain.DiscountSource bestSource = null;
        if (staffPercent.compareTo(bestPercent) > 0) {
            bestPercent = staffPercent; bestReason = "Avantage personnel : " + staff.policyLabel();
            bestSource = gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT;
        }
        if (familyPercent.compareTo(bestPercent) > 0) {
            bestPercent = familyPercent; bestReason = "Réduction familiale : " + family.ruleLabel();
            bestSource = gn.lacaeducation.billing.domain.DiscountSource.FAMILY;
        }
        if (exemptionPercent.compareTo(bestPercent) > 0) {
            bestPercent = exemptionPercent; bestReason = "Exonération : " + exemption.percentageReason();
            bestSource = gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION;
        }
        BigDecimal exceptionalAmount = exemption.exceptionalAmount() != null ? exemption.exceptionalAmount() : BigDecimal.ZERO;
        return new ApplicableInvoiceDiscount(bestPercent, bestReason, bestSource, exceptionalAmount, exemption.exceptionalReason());
    }

    /** Traduit une {@link ApplicableInvoiceDiscount} en montant réel sur un montant brut donné —
     * pourcentage gagnant + remise exceptionnelle éventuelle, plafonné au montant brut (jamais un net
     * négatif). */
    private record DiscountResult(BigDecimal amount, String reason, gn.lacaeducation.billing.domain.DiscountSource source) {}

    private DiscountResult resolveDiscount(BigDecimal grossAmount, ApplicableInvoiceDiscount d) {
        BigDecimal percentPart = grossAmount.multiply(d.percentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = percentPart.add(d.exceptionalAmount());
        if (total.compareTo(grossAmount) > 0) total = grossAmount;
        String reason = java.util.stream.Stream.of(
                        percentPart.signum() > 0 ? d.percentageReason() : null,
                        d.exceptionalAmount().signum() > 0 ? "Remise exceptionnelle : " + d.exceptionalReason() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" + "));
        gn.lacaeducation.billing.domain.DiscountSource source = percentPart.signum() > 0 ? d.source() : null;
        return new DiscountResult(total, reason.isBlank() ? null : reason, source);
    }

    @Override
    public Invoice create(CreateInvoiceCommand c) {
        String reference = (c.reference() == null || c.reference().isBlank())
                ? generateReference(c.tenantId()) : c.reference();
        Long academicYearId = resolveAcademicYearId(c.academicYearId(), c.studentId());
        BigDecimal grossAmount = c.totalAmount();
        // Réduction familiale / avantage personnel (voir computeApplicableDiscount) : appliquée ici
        // aussi (pas seulement dans generateMonthly()) pour que l'inscription, le transport, la
        // cantine et les examens en bénéficient au même titre que la mensualité — jusqu'ici seule la
        // mensualité appliquait une remise automatique, les autres frais étaient toujours facturés
        // plein tarif même pour un élève éligible.
        FeeType feeType = c.feeId() != null
                ? schoolFeeRepository.findById(c.feeId()).map(gn.lacaeducation.billing.domain.SchoolFee::getType).orElse(null)
                : null;
        var discountInfo = computeApplicableDiscount(c.tenantId(), c.studentId(), feeType);
        var discount = resolveDiscount(grossAmount, discountInfo);
        BigDecimal netAmount = grossAmount.subtract(discount.amount());
        Invoice inv = repo.save(Invoice.builder().tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                .academicYearId(academicYearId)
                .reference(reference).totalAmount(grossAmount).paidAmount(BigDecimal.ZERO).discountAmount(discount.amount())
                .discountReason(discount.reason()).discountSource(discount.source())
                .remainingBalance(netAmount).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now()).recoveryStage(RecoveryStage.NONE)
                .build());
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
        Long academicYearId = resolveAcademicYearId(c.academicYearId(), c.studentId());
        // Réduction familiale / avantage personnel (voir computeApplicableDiscount) applicable à cet
        // élève sur ces échéances — calculée une seule fois, la même pour toutes. Utilise le type RÉEL
        // du frais (ex. TRANSPORT) et non un MONTHLY codé en dur : cette méthode est aussi appelée pour
        // les frais transport (voir TransportInvoiceListener), qui ont leurs propres règles de remise.
        FeeType feeType = schoolFeeRepository.findById(c.feeId())
                .map(gn.lacaeducation.billing.domain.SchoolFee::getType).orElse(FeeType.MONTHLY);
        var discount = computeApplicableDiscount(c.tenantId(), c.studentId(), feeType);

        // Paiement mensuel (§billing, opt-in par inscription) : CE parent précis règle mois par mois —
        // une tranche multi-mois de sa classe est alors éclatée en autant de factures séparées d'un
        // mois, au lieu d'une seule facture groupée. Les autres familles de la même classe ne sont
        // jamais affectées (l'opt-in est par élève, pas par classe/frais).
        boolean monthlySplit = enrollmentQueryPort.hasMonthlyPaymentOptIn(c.studentId(), academicYearId);

        List<Invoice> created = new ArrayList<>();
        for (gn.lacaeducation.billing.domain.Installment installment : months) {
            List<String> installmentMonths = installment.getMonths();
            if (monthlySplit && installmentMonths != null && installmentMonths.size() > 1) {
                BigDecimal perMonth = installment.getAmount()
                        .divide(BigDecimal.valueOf(installmentMonths.size()), 2, java.math.RoundingMode.HALF_UP);
                for (String singleMonth : installmentMonths) {
                    String label = gn.lacaeducation.billing.domain.MonthLabel.frenchLabel(singleMonth);
                    // Idempotence par mois précis : une même échéance multi-mois produit ici plusieurs
                    // factures pour le même élève, distinguées par leur periodLabel.
                    if (repo.existsByStudentIdAndInstallmentIdAndPeriodLabel(c.studentId(), installment.getId(), label)) {
                        continue;
                    }
                    created.add(buildAndSaveInvoice(c, academicYearId, installment.getId(), label, perMonth, discount));
                }
            } else {
                // Idempotence : on ne recrée pas la facture d'une échéance déjà facturée pour cet élève.
                if (repo.existsByStudentIdAndInstallmentId(c.studentId(), installment.getId())) {
                    continue;
                }
                created.add(buildAndSaveInvoice(c, academicYearId, installment.getId(), installment.getLabel(),
                        installment.getAmount(), discount));
            }
        }
        log.info("Génération mensuelle : {} facture(s) créée(s) pour l'élève id={} frais id={}",
                created.size(), c.studentId(), c.feeId());
        return created;
    }

    private Invoice buildAndSaveInvoice(GenerateMonthlyCommand c, Long academicYearId, Long installmentId,
                                        String periodLabel, BigDecimal baseAmount,
                                        ApplicableInvoiceDiscount discountInfo) {
        var discount = resolveDiscount(baseAmount, discountInfo);
        BigDecimal net = baseAmount.subtract(discount.amount());
        String reference = generateReference(c.tenantId());
        Invoice inv = repo.save(Invoice.builder()
                .tenantId(c.tenantId()).studentId(c.studentId()).feeId(c.feeId())
                .academicYearId(academicYearId)
                .reference(reference).totalAmount(baseAmount).paidAmount(BigDecimal.ZERO).discountAmount(discount.amount())
                .discountReason(discount.reason()).discountSource(discount.source())
                .remainingBalance(net).status(InvoiceStatus.UNPAID).issueDate(LocalDate.now())
                .recoveryStage(RecoveryStage.NONE)
                .installmentId(installmentId).periodLabel(periodLabel).build());
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(c.tenantId(), c.studentId(), inv.getId(), inv.getReference()));
        eventPublisher.publishEvent(new InvoiceIssuedEvent(inv.getId(), c.studentId(), c.tenantId()));
        return inv;
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
        return applyPayment(invoiceId, amount, true, null);
    }

    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify) {
        return applyPayment(invoiceId, amount, notify, null);
    }

    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify, List<String> paidMonths) {
        Invoice inv = load(invoiceId);
        inv.setPaidAmount(inv.getPaidAmount().add(amount));
        if (paidMonths != null && !paidMonths.isEmpty()) {
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(
                    inv.getPaidMonths() != null ? inv.getPaidMonths() : List.of());
            merged.addAll(paidMonths);
            inv.setPaidMonths(new java.util.ArrayList<>(merged));
        }
        recomputeBalance(inv);
        boolean fullySettled = inv.getStatus() == InvoiceStatus.PAID;
        Invoice saved = repo.save(inv);
        if (notify) {
            eventPublisher.publishEvent(new InvoicePaymentAppliedEvent(inv.getTenantId(), inv.getStudentId(),
                    inv.getId(), inv.getReference(), amount, inv.getRemainingBalance()));
        }
        if (fullySettled) {
            eventPublisher.publishEvent(
                    new InvoicePaidEvent(inv.getTenantId(), inv.getStudentId(), inv.getId(), inv.getReference()));
        }
        // Frais plateforme (§billing) : désormais un frais annexe comme un autre, configuré par
        // l'école elle-même (FeeType.PLATFORM, nom et montant à sa discrétion) — tout encaissement sur
        // une facture de ce type alimente le solde dû à l'éditeur de la plateforme (voir
        // platform.PlatformFeeSettlementService, resté inchangé).
        if (inv.getFeeId() != null) {
            schoolFeeRepository.findById(inv.getFeeId())
                    .filter(fee -> fee.getType() == FeeType.PLATFORM)
                    .ifPresent(fee -> platformFeeLedgerApi.record(inv.getTenantId(), inv.getId(), amount));
        }
        return saved;
    }

    @Override public Invoice reversePayment(Long invoiceId, BigDecimal amount) {
        Invoice inv = load(invoiceId);
        BigDecimal currentPaid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.subtract(amount);
        // Jamais en dessous de zéro (ex. remboursement partiel supérieur à ce qui a été attribué à
        // cette facture précise dans le cas d'un paiement groupé).
        inv.setPaidAmount(newPaid.signum() < 0 ? BigDecimal.ZERO : newPaid);
        recomputeBalance(inv);
        return repo.save(inv);
    }

    @Override public Invoice voidInvoice(Long invoiceId) {
        Invoice inv = load(invoiceId);
        inv.setStatus(InvoiceStatus.CANCELLED);
        inv.setRemainingBalance(BigDecimal.ZERO);
        return repo.save(inv);
    }

    @Override public Invoice applyCredit(Long invoiceId, BigDecimal amount) {
        Invoice inv = load(invoiceId);
        BigDecimal currentPaid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
        inv.setPaidAmount(currentPaid.add(amount));
        recomputeBalance(inv);
        return repo.save(inv);
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

    @Override public void applyExternalPayment(Long tenantId, Long studentId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || studentId == null) return;
        BigDecimal remaining = amount;
        List<Invoice> outstanding = repo.findByStudentId(studentId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .filter(i -> i.getFeeId() != null && schoolFeeRepository.findById(i.getFeeId())
                        .map(f -> f.getType() == FeeType.MONTHLY).orElse(false))
                .sorted(java.util.Comparator.comparing(Invoice::getIssueDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        for (Invoice inv : outstanding) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = inv.getRemainingBalance() != null ? inv.getRemainingBalance() : BigDecimal.ZERO;
            if (due.signum() <= 0) continue;
            BigDecimal toApply = due.min(remaining);
            applyPayment(inv.getId(), toApply, false);
            remaining = remaining.subtract(toApply);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal outstandingStaffBenefitRemainder(Long studentId) {
        if (studentId == null) return BigDecimal.ZERO;
        return repo.findByStudentId(studentId).stream()
                .filter(i -> i.getDiscountSource() == gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .map(i -> i.getRemainingBalance() != null ? i.getRemainingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<Invoice> applyEarlyPaymentDiscount(Long studentId, Long feeId) {
        var fee = schoolFeeRepository.findById(feeId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", feeId));
        List<Invoice> allForFee = repo.findByStudentId(studentId).stream()
                .filter(i -> feeId.equals(i.getFeeId()))
                .toList();
        // La remise récompense le choix de payer TOUTE l'année dès le premier règlement de scolarité —
        // dès qu'une tranche a déjà été réglée (même partiellement) sans elle, il est trop tard : décider
        // ensuite de solder le reste ne doit plus donner droit à la remise.
        boolean alreadyStartedPaying = allForFee.stream()
                .anyMatch(i -> i.getPaidAmount() != null && i.getPaidAmount().signum() > 0);
        List<Invoice> outstanding = allForFee.stream()
                .filter(i -> i.getStatus() != InvoiceStatus.PAID)
                .toList();
        // Pas de « tranches » à regrouper : rien à faire (une seule facture restante, ou aucune).
        if (alreadyStartedPaying || outstanding.size() < 2) {
            return outstanding;
        }
        var discount = earlyPaymentDiscountUseCase.computeApplicable(fee.getTenantId(), fee.getType());
        if (discount.percentage() == null || discount.percentage().signum() <= 0) {
            return outstanding;
        }
        // Rien n'ayant encore été payé sur ce frais (garde ci-dessus), le total annuel et le total des
        // tranches restantes sont ici identiques — la remise porte donc sur l'année complète.
        BigDecimal outstandingTotal = outstanding.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstandingTotal.signum() <= 0) {
            return outstanding;
        }
        BigDecimal totalDiscount = outstandingTotal.multiply(discount.percentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                .min(outstandingTotal);
        // Le taux est intégré au libellé (pas seulement le nom de la règle) pour que le pourcentage de
        // remise reste visible partout où discountReason est affiché (reçu, facture, liste des élèves).
        String reason = "Remise paiement anticipé : " + discount.ruleLabel()
                + " (" + discount.percentage().stripTrailingZeros().toPlainString() + "%)";
        List<Invoice> updated = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < outstanding.size(); i++) {
            Invoice inv = outstanding.get(i);
            // Réparti au prorata du montant de chaque tranche restante ; la dernière absorbe l'arrondi
            // pour que la somme des remises corresponde exactement au total calculé.
            BigDecimal amount = i == outstanding.size() - 1
                    ? totalDiscount.subtract(allocated)
                    : totalDiscount.multiply(inv.getTotalAmount())
                            .divide(outstandingTotal, 2, java.math.RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            updated.add(applyDiscount(inv.getId(), amount, reason));
        }
        log.info("Remise paiement anticipé appliquée : {} facture(s), élève id={} frais id={}, taux={}%, base annuelle={}",
                updated.size(), studentId, feeId, discount.percentage(), outstandingTotal);
        return updated;
    }

    @Override
    public List<Invoice> removeEarlyPaymentDiscount(Long studentId, Long feeId) {
        List<Invoice> outstanding = repo.findByStudentId(studentId).stream()
                .filter(i -> feeId.equals(i.getFeeId()) && i.getStatus() != InvoiceStatus.PAID)
                .toList();
        List<Invoice> reverted = new ArrayList<>();
        for (Invoice inv : outstanding) {
            if (inv.getDiscountAmount() != null && inv.getDiscountAmount().signum() > 0) {
                reverted.add(applyDiscount(inv.getId(), BigDecimal.ZERO, null));
            } else {
                reverted.add(inv);
            }
        }
        return reverted;
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
