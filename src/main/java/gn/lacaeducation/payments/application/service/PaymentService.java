package gn.lacaeducation.payments.application.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.event.GroupedInvoiceSettlementEvent;
import gn.lacaeducation.payments.application.event.PaymentFailedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import gn.lacaeducation.payments.application.port.out.DjomyVerificationPort;
import gn.lacaeducation.payments.application.port.out.PaymentProviderGateway;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service @Transactional @RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {
    private final PaymentRepository repo;
    private final PaymentTransactionRepository transactionRepo;
    private final ReceiptUseCase receiptUseCase;
    private final List<PaymentProviderGateway> gateways;
    private final ApplicationEventPublisher eventPublisher;
    private final DjomyVerificationPort djomyVerifier;
    private final ObjectMapper objectMapper;
    private final CashierSessionRepository cashierSessionRepository;

    @Override public Payment initiate(InitiatePaymentCommand c) throws JsonProcessingException {
        // Barrière caisse (§angles morts comptables) : un encaissement guichet (espèces, Orange Money,
        // virement reçu à la main) doit être rattaché à une session de caisse ouverte pour pouvoir être
        // rapproché au comptant physique — sans ça, CashReconciliationGuard ne fait que le signaler
        // APRÈS coup, trop tard pour l'éviter.
        requireOpenCashierSession(c.channel(), c.collectedByUserId());
        // Facture → Paiement → TransactionPaiement (appel fournisseur) → EN_ATTENTE (§9.1).
        Payment payment = repo.save(Payment.builder().tenantId(c.tenantId()).invoiceId(c.invoiceId()).studentId(c.studentId())
                .academicYearId(c.academicYearId())
                .reference(c.reference()).amount(c.amount()).channel(c.channel()).type(c.type())
                .status(PaymentStatus.INITIATED).paidAt(LocalDateTime.now())
                .collectedByUserId(c.collectedByUserId()).build());

        // Encaissement manuel au guichet (espèces, Orange Money, virement) : l'argent est déjà reçu,
        // aucun fournisseur en ligne — on confirme immédiatement et on émet le reçu. L'événement
        // PaymentSucceeded met à jour la facture, exactement comme un succès en ligne.
        if (c.channel().isDeskManual()) {
            payment.transitionTo(PaymentStatus.PENDING);
            payment.transitionTo(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(LocalDateTime.now());
            Payment saved = repo.save(payment);
            receiptUseCase.generate(payment.getId());
            eventPublisher.publishEvent(new PaymentSucceededEvent(payment.getTenantId(), payment.getStudentId(),
                    payment.getId(), payment.getInvoiceId(), payment.getAmount(), payment.getCollectedByUserId()));
            return saved;
        }

        PaymentProviderGateway gateway = resolveGateway(c.channel());
        try {
            PaymentTransaction tx = gateway.initiate(payment.getId(), payment.getAmount(), payment.getReference(), c.payerNumber());
            transactionRepo.save(tx);
            payment.transitionTo(PaymentStatus.PENDING);
        } catch (RuntimeException e) {
            log.error("Provider initiation failed for payment id={} channel={}: {}", payment.getId(), c.channel(), e.getMessage());
            payment.transitionTo(PaymentStatus.FAILED);
            repo.save(payment);
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getTenantId(), payment.getStudentId(),
                    payment.getId(), payment.getInvoiceId(), payment.getAmount()));
            throw e;
        }
        return repo.save(payment);
    }

    @Override public Payment initiateGroup(GroupPaymentCommand c) {
        // Rejoué après une coupure réseau (mode hors-ligne) avec la même clé : renvoie le paiement déjà
        // créé au lieu d'en créer un second — même pattern que l'idempotence des webhooks Djomy.
        if (c.idempotencyKey() != null && !c.idempotencyKey().isBlank()) {
            Optional<Payment> existing = repo.findByIdempotencyKey(c.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (c.shares() == null || c.shares().isEmpty()) {
            throw new BusinessRuleException("Aucune facture sélectionnée pour le paiement groupé");
        }
        // Barrière caisse — sauf rejeu hors-ligne (paidAt renseigné) : l'argent a déjà été physiquement
        // encaissé au moment indiqué par paidAt, sous la session ouverte à CE moment-là côté caissier ;
        // exiger une session encore ouverte MAINTENANT (potentiellement après resynchronisation tardive,
        // caisse déjà clôturée) rejetterait à tort un encaissement pourtant légitime.
        if (c.paidAt() == null) {
            requireOpenCashierSession(c.channel(), c.collectedByUserId());
        }
        BigDecimal total = c.shares().stream()
                .map(PaymentUseCase.InvoiceShare::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String reference = "PAY-GRP-" + c.studentId() + "-"
                + Long.toString(System.currentTimeMillis(), 36).toUpperCase();

        // Fratrie (§payments) : chaque part porte son propre élève (retombe sur c.studentId() si absent,
        // paiement mono-élève classique) — studentIds retient tous les enfants couverts, dans l'ordre
        // d'apparition, pour que le reçu et l'historique de paiement de CHAQUE enfant retrouvent ce paiement.
        List<Long> distinctStudentIds = c.shares().stream()
                .map(x -> x.studentId() != null ? x.studentId() : c.studentId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // Encaissement hors-ligne (desktop) rejoué à la synchro : `paidAt` porte l'heure RÉELLE vue par
        // le caissier au moment de l'encaissement, pas celle — potentiellement bien plus tardive — de
        // cet appel réseau (§retour comptable). Un encaissement en ligne classique n'a jamais ce champ.
        LocalDateTime paidAt = c.paidAt() != null ? c.paidAt() : LocalDateTime.now();
        Payment payment = repo.save(Payment.builder()
                .tenantId(c.tenantId()).studentId(c.studentId()).studentIds(distinctStudentIds).invoiceId(null)
                .academicYearId(c.academicYearId())
                .reference(reference).amount(total).channel(c.channel()).type(PaymentType.COMPLETE)
                .status(PaymentStatus.INITIATED).paidAt(paidAt)
                .coveredMonths(c.coverageLabel()).monthsCount(c.monthsCount()).remainingAfter(c.remainingAfter())
                .groupLines(serializeShares(c.shares(), c.studentId())).collectedByUserId(c.collectedByUserId())
                .idempotencyKey(c.idempotencyKey())
                .build());

        if (c.channel().isDeskManual()) {
            // Guichet : argent déjà reçu → confirmation immédiate, reçu groupé + répartition tout de suite.
            payment.transitionTo(PaymentStatus.PENDING);
            payment.transitionTo(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(paidAt);
            Payment saved = repo.save(payment);
            settleGroup(saved);
            log.info("Paiement groupé guichet {} : {} échéance(s), total={}, élève id={}",
                    reference, c.shares().size(), total, c.studentId());
            return saved;
        }

        // En ligne (Djomy) : une seule transaction pour le total ; la répartition + le reçu groupé
        // sont produits au succès (confirmSuccess), comme pour un paiement simple.
        PaymentProviderGateway gateway = resolveGateway(c.channel());
        try {
            PaymentTransaction tx = gateway.initiate(payment.getId(), total, payment.getReference(), c.payerNumber());
            transactionRepo.save(tx);
            payment.transitionTo(PaymentStatus.PENDING);
        } catch (Exception e) {
            log.error("Échec initiation paiement groupé en ligne id={} canal={} : {}",
                    payment.getId(), c.channel(), e.getMessage());
            payment.transitionTo(PaymentStatus.FAILED);
            repo.save(payment);
            eventPublisher.publishEvent(new PaymentFailedEvent(payment.getTenantId(), payment.getStudentId(),
                    payment.getId(), null, total));
            throw new BusinessRuleException("Échec de l'initiation du paiement groupé en ligne : " + e.getMessage());
        }
        log.info("Paiement groupé en ligne {} initié : {} échéance(s), total={}, élève id={}",
                reference, c.shares().size(), total, c.studentId());
        return repo.save(payment);
    }

    /** Reçu groupé unique + comptabilisation (une fois) + répartition sur chaque facture-mois. */
    private void settleGroup(Payment p) {
        receiptUseCase.generateGrouped(p.getId(), p.getCoveredMonths(), p.getMonthsCount(), p.getRemainingAfter());
        eventPublisher.publishEvent(new PaymentSucceededEvent(p.getTenantId(), p.getStudentId(),
                p.getId(), null, p.getAmount(), p.getCollectedByUserId()));
        eventPublisher.publishEvent(new GroupedInvoiceSettlementEvent(
                p.getTenantId(), p.getStudentId(), p.getId(), deserializeShares(p.getGroupLines()),
                p.getCollectedByUserId()));
    }

    private String serializeShares(List<PaymentUseCase.InvoiceShare> shares, Long fallbackStudentId) {
        List<GroupedInvoiceSettlementEvent.Share> s = shares.stream()
                .map(x -> new GroupedInvoiceSettlementEvent.Share(
                        x.invoiceId(), x.studentId() != null ? x.studentId() : fallbackStudentId, x.amount(), x.paidMonths()))
                .toList();
        try {
            return objectMapper.writeValueAsString(s);
        } catch (RuntimeException e) {
            throw new BusinessRuleException("Sérialisation des parts du paiement groupé impossible");
        }
    }

    private List<GroupedInvoiceSettlementEvent.Share> deserializeShares(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<GroupedInvoiceSettlementEvent.Share>>() {});
        } catch (RuntimeException e) {
            throw new BusinessRuleException("Lecture des parts du paiement groupé impossible");
        }
    }

    @Override @Transactional(readOnly = true)
    public DjomyStatusResponse checkDjomyStatus(Long paymentId) throws JsonProcessingException {
        load(paymentId); // 404 if the payment does not exist
        String externalId = transactionRepo.findByPaymentId(paymentId).stream()
                .filter(t -> t.getProvider() == PaymentProvider.DJOMY)
                .map(PaymentTransaction::getExternalTransactionId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Payment " + paymentId + " has no Djomy transaction to query"));
        DjomyVerificationPort.DjomyStatus s = djomyVerifier.verify(externalId);
        return new DjomyStatusResponse(paymentId, externalId, s.status(), s.amount(), s.currency(), s.success());
    }

    @Override public Payment confirmSuccess(Long id) {
        Payment p = load(id);
        if (p.getStatus() == PaymentStatus.SUCCEEDED) {
            return p; // idempotent : déjà confirmé (webhook + vérification à la demande)
        }
        p.transitionTo(PaymentStatus.SUCCEEDED);
        p.setPaidAt(LocalDateTime.now());
        Payment saved = repo.save(p);
        if (p.getGroupLines() != null && !p.getGroupLines().isBlank()) {
            // Paiement groupé (plusieurs mois) : reçu groupé unique + répartition sur les factures-mois.
            settleGroup(saved);
        } else {
            // Reçu généré seulement sur un paiement réussi (§9.3), idempotent.
            receiptUseCase.generate(p.getId());
            eventPublisher.publishEvent(new PaymentSucceededEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount(), p.getCollectedByUserId()));
        }
        return saved;
    }

    @Override public Payment confirmViaDjomy(Long id) throws JsonProcessingException {
        Payment p = load(id);
        if (p.getStatus() == PaymentStatus.SUCCEEDED) {
            return p; // idempotent
        }
        String externalId = transactionRepo.findByPaymentId(id).stream()
                .filter(t -> t.getProvider() == PaymentProvider.DJOMY)
                .map(PaymentTransaction::getExternalTransactionId)
                .filter(x -> x != null && !x.isBlank())
                .findFirst().orElse(null);
        if (externalId == null) {
            return p; // pas (encore) de transaction Djomy à vérifier
        }
        // Activation automatique « comme le webhook » : on confirme dès que Djomy reporte le succès.
        if (djomyVerifier.verify(externalId).success()) {
            log.info("Confirmation automatique du paiement {} via statut Djomy (tx={})", id, externalId);
            return confirmSuccess(id);
        }
        return p;
    }

    @Override public Payment markFailed(Long id) {
        Payment p = load(id);
        p.transitionTo(PaymentStatus.FAILED);
        Payment saved = repo.save(p);
        eventPublisher.publishEvent(new PaymentFailedEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }

    @Override public Payment markRefunded(Long id) {
        Payment p = load(id);
        p.transitionTo(PaymentStatus.REFUNDED);
        Payment saved = repo.save(p);
        eventPublisher.publishEvent(new PaymentRefundedEvent(p.getTenantId(), p.getStudentId(), p.getId(), p.getInvoiceId(), p.getAmount()));
        return saved;
    }

    @Override @Transactional(readOnly=true) public Optional<Payment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public Optional<Payment> findByReference(String ref) { return repo.findByReference(ref); }
    @Override @Transactional(readOnly=true) public List<Payment> findByInvoiceId(Long inv) { return repo.findByInvoiceId(inv); }
    @Override @Transactional(readOnly=true) public List<Payment> findByStudentId(Long s) { return repo.findByStudentId(s); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantIdAndStatus(Long t, PaymentStatus s) { return repo.findByTenantIdAndStatus(t, s); }
    @Override @Transactional(readOnly=true) public List<Payment> findByTenantIdAndAcademicYearId(Long t, Long y) { return repo.findByTenantIdAndAcademicYearId(t, y); }
    @Override @Transactional(readOnly = true)
    public CashierCollectionsSummary cashierCollectionsSummary(Long cashierId, LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = repo.findByCollectedByUserIdAndPaidAtBetween(cashierId, from, to);
        BigDecimal total = payments.stream().map(Payment::getAmount).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashierCollectionsSummary(cashierId, total, payments);
    }
    @Override @Transactional(readOnly = true)
    public DailyCollectionsReport dailyCollectionsReport(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments = repo.findByPaidAtBetween(from, to).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .toList();
        java.util.Map<java.time.LocalDate, List<Payment>> byDay = payments.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getPaidAt().toLocalDate()));
        List<DailyCollectionsLine> days = byDay.entrySet().stream()
                .map(e -> new DailyCollectionsLine(e.getKey(),
                        e.getValue().stream().map(Payment::getAmount).filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()))
                .sorted(java.util.Comparator.comparing(DailyCollectionsLine::date))
                .toList();
        BigDecimal grandTotal = days.stream().map(DailyCollectionsLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyCollectionsReport(grandTotal, days);
    }
    @Override public void delete(Long id) { repo.deleteById(id); }

    /** Encaissement/décaissement guichet (espèces, Orange Money, virement reçu à la main) : impossible
     * sans session de caisse ouverte pour le collecteur. Les canaux en ligne (Djomy...) ne sont jamais
     * concernés — l'argent transite par la passerelle, jamais par la caisse physique. */
    private void requireOpenCashierSession(PaymentChannel channel, Long collectedByUserId) {
        if (channel == null || !channel.isDeskManual() || collectedByUserId == null) return;
        if (cashierSessionRepository.findOpenByCashierId(collectedByUserId).isEmpty()) {
            throw new BusinessRuleException(
                    "Aucune session de caisse ouverte : ouvrez votre caisse avant d'enregistrer un encaissement au guichet.");
        }
    }

    private PaymentProviderGateway resolveGateway(PaymentChannel channel) {
        return gateways.stream().filter(g -> g.getChannel() == channel).findFirst()
                .orElseThrow(() -> new BusinessRuleException("No payment provider configured for channel " + channel));
    }

    private Payment load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id)); }
}
