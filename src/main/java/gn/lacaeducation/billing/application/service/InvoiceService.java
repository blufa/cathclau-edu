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
    private final gn.lacaeducation.billing.application.port.in.ExemptionUseCase exemptionUseCase;
    private final gn.lacaeducation.billing.application.port.in.EarlyPaymentDiscountUseCase earlyPaymentDiscountUseCase;
    private final gn.lacaeducation.billing.application.port.in.StaffChildBenefitUseCase staffChildBenefitUseCase;
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
     * Remise applicable à une facture — exonération TOTAL/PARTIAL choisie manuellement par le
     * comptable (§8, catalogue {@code ExemptionRule}). Les mécanismes de détection automatique
     * (réduction familiale par fratrie réelle, avantage personnel par lien parent↔enseignant) ont été
     * retirés du calcul (§retour comptable) : le comptable connaît la situation de chaque enfant à
     * l'inscription et choisit lui-même l'exonération adaptée dans la liste, y compris pour un
     * "avantage personnel" ou une "remise fratrie" — ces cas restent possibles via le catalogue, mais
     * ne sont plus jamais déclenchés automatiquement, même quand la situation change en cours d'année
     * (un nouvel enfant inscrit n'affecte jamais rétroactivement les factures déjà générées des autres
     * enfants). Une exonération EXCEPTIONAL (montant fixe, décision ponctuelle documentée) s'ajoute EN
     * PLUS du pourcentage gagnant. {@code feeType == null} (fee introuvable/non précisé) ne déclenche
     * aucune remise.
     */
    private record ApplicableInvoiceDiscount(BigDecimal percentage, String percentageReason,
                                              gn.lacaeducation.billing.domain.DiscountSource source,
                                              BigDecimal exceptionalAmount, String exceptionalReason) {}

    private ApplicableInvoiceDiscount computeApplicableDiscount(Long tenantId, Long studentId, FeeType feeType) {
        if (feeType == null) return new ApplicableInvoiceDiscount(BigDecimal.ZERO, null, null, BigDecimal.ZERO, null);
        var exemption = exemptionUseCase.computeApplicable(studentId, feeType);

        BigDecimal exemptionPercent = exemption.percentage() != null ? exemption.percentage() : BigDecimal.ZERO;
        BigDecimal bestPercent = BigDecimal.ZERO;
        String bestReason = null;
        gn.lacaeducation.billing.domain.DiscountSource bestSource = null;
        if (exemptionPercent.compareTo(bestPercent) > 0) {
            bestPercent = exemptionPercent;
            // Pourcentage intégré au libellé (même principe que la remise paiement anticipé juste au-dessus
            // dans ce fichier) — sans lui, le reçu/la facture n'affichaient que le nom de l'exonération et
            // le montant déduit, jamais le taux lui-même (§retour comptable).
            bestReason = (exemption.staffBenefit() ? "Avantage personnel : " : "Exonération : ") + exemption.percentageReason()
                    + " (" + bestPercent.stripTrailingZeros().toPlainString() + "%)";
            bestSource = exemption.staffBenefit()
                    ? gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT
                    : gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION;
        }
        // Enfant du personnel SANS aucune remise (ni exonération manuelle ci-dessus, quel que soit son
        // rang dans la politique de prise en charge — retour comptable : "quelle qu'en soit le nombre")
        // sur CE frais précisément, quel que soit son type (scolarité, inscription, transport...) :
        // tagué avantage personnel à 0% quand même, pour que le prélèvement salarial automatique
        // (SalaryFeeDeductionService#computeStaffBenefitRemainder) prenne en charge le montant COMPLET,
        // sans action manuelle du comptable facture par facture — retour comptable : "qu'il récupère
        // tout" (y compris les frais d'inscription, que des enseignants évitaient en s'inscrivant eux-
        // mêmes comme parent sans jamais payer). Ne touche jamais un enfant qui a déjà une remise par
        // ailleurs (bestSource non nul), ni un enfant dont le parent personnel a explicitement choisi de
        // payer lui-même (voir StaffChildBenefitService#isStaffChild).
        if (bestSource == null && staffChildBenefitUseCase.isStaffChild(studentId)) {
            bestReason = "Avantage personnel : enfant du personnel — frais prélevés sur le salaire";
            bestSource = gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT;
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
        // `percentageReason`/`source` sont fiables tels quels (voir computeApplicableDiscount) : jamais
        // renseignés sans raison valable — y compris à 0% pour un enfant du personnel sans remise, tagué
        // exprès pour le prélèvement salarial (voir plus haut), donc PAS conditionnés à percentPart > 0
        // ici (sans quoi ce tag à 0% disparaîtrait silencieusement).
        String reason = java.util.stream.Stream.of(
                        d.percentageReason(),
                        d.exceptionalAmount().signum() > 0 ? "Remise exceptionnelle : " + d.exceptionalReason() : null)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" + "));
        gn.lacaeducation.billing.domain.DiscountSource source = d.source();
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

    @Override public GenerateMonthlyResult generateMonthly(GenerateMonthlyCommand c) {
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
        List<ExemptedPeriod> exempted = new ArrayList<>();
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
                    buildAndSaveInvoice(c, academicYearId, installment.getId(), label, perMonth, discount, created, exempted);
                }
            } else {
                // Idempotence : on ne recrée pas la facture d'une échéance déjà facturée pour cet élève.
                if (repo.existsByStudentIdAndInstallmentId(c.studentId(), installment.getId())) {
                    continue;
                }
                buildAndSaveInvoice(c, academicYearId, installment.getId(), installment.getLabel(),
                        installment.getAmount(), discount, created, exempted);
            }
        }
        log.info("Génération mensuelle : {} facture(s) créée(s), {} échéance(s) exonérée(s) à 100% pour l'élève id={} frais id={}",
                created.size(), exempted.size(), c.studentId(), c.feeId());
        return new GenerateMonthlyResult(created, exempted);
    }

    @Override public GenerateMonthlyResult generateSelectedInstallments(GenerateMonthlyCommand c, List<Long> installmentIds) {
        if (!enrollmentQueryPort.hasActiveEnrollment(c.studentId())) {
            throw new BusinessRuleException("L'élève n'est pas inscrit : impossible de générer une échéance. "
                    + "Inscrivez-le d'abord dans une classe.");
        }
        if (installmentIds == null || installmentIds.isEmpty()) {
            throw new BusinessRuleException("Aucune échéance sélectionnée.");
        }
        java.util.Set<Long> requested = new java.util.HashSet<>(installmentIds);
        List<gn.lacaeducation.billing.domain.Installment> selected = installmentRepository.findByFeeId(c.feeId()).stream()
                .filter(m -> requested.contains(m.getId()))
                .toList();
        if (selected.isEmpty()) {
            throw new BusinessRuleException("Aucune échéance (mois) configurée pour le frais id=" + c.feeId()
                    + " ne correspond à la sélection.");
        }
        Long academicYearId = resolveAcademicYearId(c.academicYearId(), c.studentId());
        FeeType feeType = schoolFeeRepository.findById(c.feeId())
                .map(gn.lacaeducation.billing.domain.SchoolFee::getType).orElse(FeeType.MONTHLY);
        var discount = computeApplicableDiscount(c.tenantId(), c.studentId(), feeType);
        List<Invoice> created = new ArrayList<>();
        List<ExemptedPeriod> exempted = new ArrayList<>();
        for (gn.lacaeducation.billing.domain.Installment installment : selected) {
            // Idempotent : une échéance déjà facturée (ex. cliquée deux fois, ou déjà réglée un mois
            // précédent) est silencieusement ignorée plutôt que de produire une erreur ou un doublon.
            if (repo.existsByStudentIdAndInstallmentId(c.studentId(), installment.getId())) continue;
            buildAndSaveInvoice(c, academicYearId, installment.getId(), installment.getLabel(), installment.getAmount(), discount, created, exempted);
        }
        log.info("Facturation au mois : {} facture(s) créée(s), {} échéance(s) exonérée(s) à 100% pour l'élève id={} frais id={} (sélection : {})",
                created.size(), exempted.size(), c.studentId(), c.feeId(), installmentIds);
        return new GenerateMonthlyResult(created, exempted);
    }

    /** Ajoute soit une Invoice réelle à {@code created}, soit — quand l'échéance est entièrement
     * couverte par une exonération/avantage personnel (discountSource non nul, net dû = 0) — une
     * {@link ExemptedPeriod} à {@code exempted} : aucune facture à 0 GNF n'est jamais créée, pour
     * qu'elle ne puisse jamais se retrouver cochée par erreur dans un encaissement groupé (§billing,
     * retour comptable — voir aussi la garde équivalente dans applyEarlyPaymentDiscount).
     */
    private void buildAndSaveInvoice(GenerateMonthlyCommand c, Long academicYearId, Long installmentId,
                                        String periodLabel, BigDecimal baseAmount,
                                        ApplicableInvoiceDiscount discountInfo,
                                        List<Invoice> created, List<ExemptedPeriod> exempted) {
        var discount = resolveDiscount(baseAmount, discountInfo);
        BigDecimal net = baseAmount.subtract(discount.amount());
        if (discount.source() != null && net.signum() <= 0) {
            exempted.add(new ExemptedPeriod(installmentId, periodLabel, discount.reason()));
            return;
        }
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
        created.add(inv);
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
        return applyPayment(invoiceId, amount, notify, paidMonths, null);
    }

    @Override public Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify, List<String> paidMonths, Long collectedByUserId) {
        Invoice inv = load(invoiceId);
        // Trop-perçu (§4 cahier des charges) : un encaissement supérieur au solde dû ne doit plus
        // gonfler paidAmount au-delà du total (remainingBalance négatif, perdu dans la nature) — on
        // plafonne ce qui est imputé à CETTE facture à son solde dû avant ce paiement, et l'excédent
        // part en StudentCreditOverpaidEvent (posé comme crédit disponible de l'élève, applicable à
        // une facture future). Le Payment/reçu d'origine, lui, garde bien le montant RÉELLEMENT reçu
        // (amount, non plafonné) — seule l'imputation à cette facture précise est plafonnée.
        BigDecimal remainingBefore = inv.getRemainingBalance() != null ? inv.getRemainingBalance() : amount;
        BigDecimal appliedToThisInvoice = amount;
        BigDecimal excess = BigDecimal.ZERO;
        if (remainingBefore.signum() >= 0 && amount.compareTo(remainingBefore) > 0) {
            appliedToThisInvoice = remainingBefore;
            excess = amount.subtract(remainingBefore);
        }
        inv.setPaidAmount(inv.getPaidAmount().add(appliedToThisInvoice));
        if (paidMonths != null && !paidMonths.isEmpty()) {
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(
                    inv.getPaidMonths() != null ? inv.getPaidMonths() : List.of());
            merged.addAll(paidMonths);
            inv.setPaidMonths(new java.util.ArrayList<>(merged));
        }
        if (collectedByUserId != null) {
            inv.setCollectedByUserId(collectedByUserId);
            // Le PDF déjà généré/mis en cache (voir pdfUrl) ne reflète pas encore ce nouvel
            // encaisseur : on force sa régénération au prochain accès plutôt que de laisser un PDF
            // périmé en circulation.
            inv.setPdfFile(null);
        }
        recomputeBalance(inv);
        boolean fullySettled = inv.getStatus() == InvoiceStatus.PAID;
        Invoice saved = repo.save(inv);
        if (notify) {
            eventPublisher.publishEvent(new InvoicePaymentAppliedEvent(inv.getTenantId(), inv.getStudentId(),
                    inv.getId(), inv.getReference(), appliedToThisInvoice, inv.getRemainingBalance(), collectedByUserId));
        }
        if (fullySettled) {
            eventPublisher.publishEvent(
                    new InvoicePaidEvent(inv.getTenantId(), inv.getStudentId(), inv.getId(), inv.getReference()));
        }
        if (excess.signum() > 0) {
            eventPublisher.publishEvent(new gn.lacaeducation.billing.application.event.InvoiceOverpaidEvent(
                    inv.getTenantId(), inv.getStudentId(), inv.getId(), null, excess));
        }
        // Frais plateforme (§billing) : désormais un frais annexe comme un autre, configuré par
        // l'école elle-même (FeeType.PLATFORM, nom et montant à sa discrétion) — tout encaissement sur
        // une facture de ce type alimente le solde dû à l'éditeur de la plateforme (voir
        // platform.PlatformFeeSettlementService, resté inchangé). Montant réellement imputé à CETTE
        // facture (jamais l'excédent parti en crédit, qui alimentera son propre frais plateforme le
        // jour où il sera appliqué à une facture future).
        if (inv.getFeeId() != null) {
            BigDecimal platformFeeBasis = appliedToThisInvoice;
            schoolFeeRepository.findById(inv.getFeeId())
                    .filter(fee -> fee.getType() == FeeType.PLATFORM)
                    .ifPresent(fee -> platformFeeLedgerApi.record(inv.getTenantId(), inv.getId(), platformFeeBasis));
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

    @Override public Invoice applyCredit(Long invoiceId, BigDecimal amount, String reason) {
        Invoice inv = load(invoiceId);
        BigDecimal currentPaid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
        inv.setPaidAmount(currentPaid.add(amount));
        BigDecimal currentCredited = inv.getCreditedAmount() != null ? inv.getCreditedAmount() : BigDecimal.ZERO;
        inv.setCreditedAmount(currentCredited.add(amount));
        inv.setCreditReason(reason);
        inv.setCreditedAt(java.time.LocalDateTime.now());
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
    public BigDecimal outstandingStaffBenefitRemainder(Long studentId, java.time.LocalDate periodStart, String periodLabel) {
        if (studentId == null) return BigDecimal.ZERO;
        String targetLabel = resolveTargetPeriodLabel(periodStart, periodLabel);
        return staffBenefitInvoicesForPeriod(studentId, targetLabel).stream()
                .map(i -> i.getRemainingBalance() != null ? i.getRemainingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void applyStaffBenefitRemainderPayment(Long studentId, java.time.LocalDate periodStart, String periodLabel, BigDecimal amount) {
        if (studentId == null || amount == null || amount.signum() <= 0) return;
        String targetLabel = resolveTargetPeriodLabel(periodStart, periodLabel);
        BigDecimal remaining = amount;
        for (Invoice inv : staffBenefitInvoicesForPeriod(studentId, targetLabel)) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = inv.getRemainingBalance() != null ? inv.getRemainingBalance() : BigDecimal.ZERO;
            if (due.signum() <= 0) continue;
            BigDecimal toApply = due.min(remaining);
            applyPayment(inv.getId(), toApply, false);
            remaining = remaining.subtract(toApply);
        }
    }

    /** Mois "Octobre 2026" dérivé de {@code periodStart} si fourni (même format que
     * {@link Invoice#getPeriodLabel()} pour une mensualité éclatée, voir {@code generateMonthly}) ;
     * repli sur le libellé de période tel quel sinon (la paie n'a pas toujours une date saisie). */
    private String resolveTargetPeriodLabel(java.time.LocalDate periodStart, String periodLabel) {
        if (periodStart == null) return periodLabel;
        String yyyyMm = String.format("%04d-%02d", periodStart.getYear(), periodStart.getMonthValue());
        return gn.lacaeducation.billing.domain.MonthLabel.frenchLabel(yyyyMm);
    }

    private static final java.util.regex.Pattern MONTH_LABEL_PATTERN = java.util.regex.Pattern.compile(
            "(?i)^(janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre) \\d{4}$");

    /** Factures STAFF_BENEFIT encore dues de l'élève correspondant au mois {@code targetLabel} — une
     * facture dont le libellé de période ne ressemble pas à un mois précis ("Mois Année", produit par
     * l'éclatement mensuel) n'est rattachée à aucun mois en particulier : elle reste comptée quel que
     * soit le mois demandé (tranche non éclatée, transport, cantine, examen…). */
    private List<Invoice> staffBenefitInvoicesForPeriod(Long studentId, String targetLabel) {
        return repo.findByStudentId(studentId).stream()
                .filter(i -> i.getDiscountSource() == gn.lacaeducation.billing.domain.DiscountSource.STAFF_BENEFIT)
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .filter(i -> {
                    String label = i.getPeriodLabel();
                    if (label == null || !MONTH_LABEL_PATTERN.matcher(label).matches()) return true;
                    return targetLabel != null && label.equalsIgnoreCase(targetLabel);
                })
                .toList();
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
        // Une tranche déjà couverte par une exonération/avantage personnel (discountSource non nul,
        // décidé une fois pour toutes par le comptable à l'inscription — voir computeApplicableDiscount)
        // ne doit JAMAIS être touchée ici : `applyDiscount` REMPLACE la remise existante au lieu de s'y
        // ajouter, donc lui appliquer la remise paiement anticipé effacerait purement et simplement
        // l'exonération (bug vécu en vrai : une exonération 100% "prise en charge totale" remplacée par
        // seulement -5%, l'élève se retrouvant à devoir payer 95% du tarif plein). Base de calcul et
        // application restreintes aux tranches encore au tarif plein.
        List<Invoice> eligible = outstanding.stream()
                .filter(i -> i.getDiscountSource() == null)
                .toList();
        if (eligible.size() < 2) {
            return outstanding;
        }
        var discount = earlyPaymentDiscountUseCase.computeApplicable(fee.getTenantId(), fee.getType());
        if (discount.percentage() == null || discount.percentage().signum() <= 0) {
            return outstanding;
        }
        // Rien n'ayant encore été payé sur ce frais (garde ci-dessus), le total annuel et le total des
        // tranches restantes sont ici identiques — la remise porte donc sur l'année complète.
        BigDecimal outstandingTotal = eligible.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        java.util.Map<Long, Invoice> updatedById = new java.util.LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < eligible.size(); i++) {
            Invoice inv = eligible.get(i);
            // Réparti au prorata du montant de chaque tranche restante éligible ; la dernière absorbe
            // l'arrondi pour que la somme des remises corresponde exactement au total calculé.
            BigDecimal amount = i == eligible.size() - 1
                    ? totalDiscount.subtract(allocated)
                    : totalDiscount.multiply(inv.getTotalAmount())
                            .divide(outstandingTotal, 2, java.math.RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            updatedById.put(inv.getId(), applyDiscount(inv.getId(), amount, reason));
        }
        log.info("Remise paiement anticipé appliquée : {} facture(s), élève id={} frais id={}, taux={}%, base annuelle={}",
                updatedById.size(), studentId, feeId, discount.percentage(), outstandingTotal);
        // Renvoie la liste complète du frais (tranches remisées + tranches déjà exonérées inchangées) :
        // le front indexe par id sur cette réponse et doit retrouver TOUTES les factures, pas seulement
        // celles modifiées ici.
        return outstanding.stream().map(i -> updatedById.getOrDefault(i.getId(), i)).toList();
    }

    @Override
    public List<Invoice> removeEarlyPaymentDiscount(Long studentId, Long feeId) {
        List<Invoice> outstanding = repo.findByStudentId(studentId).stream()
                .filter(i -> feeId.equals(i.getFeeId()) && i.getStatus() != InvoiceStatus.PAID)
                .toList();
        List<Invoice> reverted = new ArrayList<>();
        for (Invoice inv : outstanding) {
            // Même garde que dans applyEarlyPaymentDiscount : ne jamais effacer la remise d'une tranche
            // exonérée/avantage personnel — sa remise ne vient jamais de ce mécanisme, l'annuler ici
            // supprimerait l'exonération elle-même au lieu de simplement retirer -5%.
            boolean fromThisMechanism = inv.getDiscountSource() == null
                    && inv.getDiscountAmount() != null && inv.getDiscountAmount().signum() > 0;
            if (fromThisMechanism) {
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

    /** "Annuler la facture" (écran Factures élèves) : conserve l'historique — même comportement que
     * {@link #voidInvoice}, jamais une suppression physique (régression réelle corrigée : cette
     * méthode faisait jusqu'ici un repo.deleteById, perdant irrémédiablement la facture malgré son
     * nom et malgré le commentaire côté client qui annonçait l'historique conservé). */
    @Override public void cancel(Long id) { voidInvoice(id); }

    /** Suppression PHYSIQUE d'une facture — uniquement si elle n'a encore reçu aucun paiement
     * (§retour comptable : permettre de nettoyer une facture générée par erreur, ex. doublon, tant
     * qu'elle n'est pas encaissée). Avant ce correctif, rien ne l'empêchait côté serveur — seul un
     * contrôle client (contournable) existait ; une facture déjà payée aurait pu être supprimée sans
     * trace. Utiliser {@link #cancel} pour une facture déjà encaissée (historique conservé). */
    @Override public void delete(Long id) {
        Invoice inv = load(id);
        BigDecimal paid = inv.getPaidAmount();
        if (paid != null && paid.signum() > 0) {
            throw new BusinessRuleException(
                    "Impossible de supprimer cette facture : un paiement a déjà été encaissé dessus. "
                            + "Utilisez \"Annuler\" si besoin (conserve l'historique).");
        }
        repo.deleteById(id);
    }
    @Override @Transactional(readOnly=true) public boolean existsById(Long id) { return repo.existsById(id); }
    private Invoice load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id)); }
}
