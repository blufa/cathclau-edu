package gn.lacaeducation.payments.adapters.in.web;
import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.application.port.in.PaymentTransactionUseCase;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Tag(name = "Payments", description = "Initier et gérer les paiements des frais scolaires via mobile money et carte bancaire")
@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase useCase;
    private final PaymentTransactionUseCase transactionUseCase;
    private final ReceiptUseCase receiptUseCase;
    public record InitiateRequest(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type,
                                  String payerNumber, Long academicYearId) {}
    /** Initiation result: the payment plus the aggregator redirect URL (Djomy) when present. */
    public record InitiateResponse(Payment payment, String redirectUrl) {}
    @Operation(summary = "Initier un nouveau paiement pour une facture élève (retourne redirectUrl pour les canaux agrégateurs)")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PostMapping public ResponseEntity<InitiateResponse> initiate(@RequestBody InitiateRequest r, @AuthenticationPrincipal Jwt jwt) throws JsonProcessingException {
        return ResponseEntity.ok(doInitiate(r, currentUserId(jwt)));
    }

    public record GroupRequest(Long tenantId, Long studentId, PaymentChannel channel, String payerNumber,
                               String coverageLabel, Integer monthsCount, java.math.BigDecimal remainingAfter,
                               java.util.List<PaymentUseCase.InvoiceShare> shares, Long academicYearId,
                               /** Clé d'idempotence fournie par le client (mode hors-ligne, encaissement rejoué
                                *  après une coupure réseau) — voir {@link PaymentUseCase.GroupPaymentCommand}. */
                               String idempotencyKey) {}

    @Operation(summary = "Paiement groupé de plusieurs mois / de l'année entière — un paiement, un reçu. "
            + "Les canaux guichet sont immédiats ; Djomy retourne une redirectUrl pour une transaction en ligne unique.")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PostMapping("/group") public ResponseEntity<InitiateResponse> group(@RequestBody GroupRequest r, @AuthenticationPrincipal Jwt jwt) {
        Payment payment = useCase.initiateGroup(new PaymentUseCase.GroupPaymentCommand(
                r.tenantId(), r.studentId(), r.channel(), r.payerNumber(),
                r.coverageLabel(), r.monthsCount(), r.remainingAfter(), r.shares(), currentUserId(jwt), r.academicYearId(),
                r.idempotencyKey()));
        String redirectUrl = transactionUseCase.findByPaymentId(payment.getId()).stream()
                .map(PaymentTransaction::getRedirectUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null);
        return ResponseEntity.ok(new InitiateResponse(payment, redirectUrl));
    }

    /** Utilisateur connecté ayant saisi l'encaissement (« Opérateur » sur le reçu) — extrait du JWT (sub). */
    private static Long currentUserId(Jwt jwt) {
        try {
            return jwt == null ? null : Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Operation(summary = "Confirmer un paiement en interrogeant Djomy à la demande (sans webhook) : en cas de succès, "
            + "confirme le paiement, répartit un groupe sur ses factures mensuelles et émet le reçu. Idempotent.")
    @PreAuthorize("hasAuthority('paiement:create') or hasRole('PARENT') or hasRole('STUDENT')")
    @PostMapping("/{id}/confirm-djomy") public ResponseEntity<Payment> confirmViaDjomy(@PathVariable Long id) throws JsonProcessingException {
        return ResponseEntity.ok(useCase.confirmViaDjomy(id));
    }

    @Operation(summary = "Paiement en ligne self-service par un parent/élève (canaux en ligne uniquement — pas d'espèces)")
    @PreAuthorize("hasAnyRole('PARENT','STUDENT')")
    @PostMapping("/self-service") public ResponseEntity<InitiateResponse> selfService(@RequestBody InitiateRequest r) throws JsonProcessingException {
        if (r.channel().isDeskManual()) {
            throw new gn.lacaeducation.common.BusinessRuleException(
                    "Ce mode d'encaissement est réservé au guichet ; en ligne, utilisez le portail Djomy");
        }
        return ResponseEntity.ok(doInitiate(r, null)); // paiement en ligne par le parent/élève lui-même : pas d'« opérateur » guichet
    }

    private InitiateResponse doInitiate(InitiateRequest r, Long collectedByUserId) throws JsonProcessingException {
        Payment payment = useCase.initiate(new PaymentUseCase.InitiatePaymentCommand(
                r.tenantId(), r.invoiceId(), r.studentId(), r.reference(), r.amount(), r.channel(), r.type(), r.payerNumber(), collectedByUserId, r.academicYearId()));
        String redirectUrl = transactionUseCase.findByPaymentId(payment.getId()).stream()
                .map(PaymentTransaction::getRedirectUrl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null);
        return new InitiateResponse(payment, redirectUrl);
    }
    @Operation(summary = "Consulter le statut Djomy en temps réel d'un paiement (diagnostic : suivre CREATED → PENDING → SUCCESS sans attendre le webhook)")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping("/{id}/djomy-status") public ResponseEntity<PaymentUseCase.DjomyStatusResponse> djomyStatus(@PathVariable Long id) throws JsonProcessingException {
        return ResponseEntity.ok(useCase.checkDjomyStatus(id));
    }
    @Operation(summary = "Confirmer un paiement comme réussi")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PutMapping("/{id}/success") public ResponseEntity<Payment> confirmSuccess(@PathVariable Long id) { return ResponseEntity.ok(useCase.confirmSuccess(id)); }
    @Operation(summary = "Marquer un paiement comme échoué")
    @PreAuthorize("hasAuthority('paiement:create')")
    @PutMapping("/{id}/fail") public ResponseEntity<Payment> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @Operation(summary = "Marquer un paiement comme remboursé")
    @PreAuthorize("hasAuthority('paiement:refund')")
    @PutMapping("/{id}/refund") public ResponseEntity<Payment> markRefunded(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRefunded(id)); }
    @Operation(summary = "Consulter un paiement par ID")
    @GetMapping("/{id}") public ResponseEntity<Payment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Consulter un paiement par son numéro de référence")
    @GetMapping("/reference/{reference}") public ResponseEntity<Payment> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Consulter les transactions fournisseur associées à un paiement (§9.1)")
    @PreAuthorize("hasAuthority('transaction:read')")
    @GetMapping("/{id}/transactions") public List<PaymentTransaction> transactions(@PathVariable Long id) {
        return transactionUseCase.findByPaymentId(id);
    }
    @Operation(summary = "Consulter l'URL du reçu PDF d'un paiement réussi (§9.1) — généré à la demande si absent")
    @GetMapping("/{id}/receipt/pdf") public ResponseEntity<Map<String, String>> receiptPdf(@PathVariable Long id) {
        return receiptUseCase.findByPaymentId(id)
                .map(r -> ResponseEntity.ok(Map.of("url", receiptUseCase.pdfUrl(r.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Diffuser les octets du reçu PDF (inline) pour un paiement — indépendant de Cloudinary")
    @GetMapping("/{id}/receipt/pdf/download") public ResponseEntity<byte[]> receiptPdfDownload(@PathVariable Long id) {
        return receiptUseCase.findByPaymentId(id)
                .map(r -> ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + r.getId() + ".pdf\"")
                        .body(receiptUseCase.pdfBytes(r.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Lister tous les paiements d'une facture")
    @GetMapping("/invoice/{invoiceId}") public List<Payment> findByInvoice(@PathVariable Long invoiceId) { return useCase.findByInvoiceId(invoiceId); }
    @Operation(summary = "Lister tous les paiements d'un élève (personnel)")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping("/student/{studentId}") public List<Payment> findByStudent(@PathVariable Long studentId) {
        return useCase.findByStudentId(studentId);
    }
    @Operation(summary = "Lister tous les paiements d'un tenant — passer academicYearId pour se limiter à une année scolaire (recommandé : un appel sans filtre mélange toutes les années créées)")
    @GetMapping("/tenant/{tenantId}") public List<Payment> findByTenant(@PathVariable Long tenantId, @RequestParam(required = false) Long academicYearId) {
        return academicYearId != null ? useCase.findByTenantIdAndAcademicYearId(tenantId, academicYearId) : useCase.findByTenantId(tenantId);
    }
    @Operation(summary = "Rapport « encaissements par comptable » : total et détail des paiements CASH "
            + "enregistrés par un utilisateur (guichet) sur une plage de dates — rapprochement de caisse")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping("/reports/by-cashier")
    public PaymentUseCase.CashierCollectionsSummary cashierCollectionsSummary(
            @RequestParam Long cashierId,
            @RequestParam java.time.LocalDateTime from,
            @RequestParam java.time.LocalDateTime to) {
        return useCase.cashierCollectionsSummary(cashierId, from, to);
    }
    @Operation(summary = "Rapport « encaissements par jour » : total et nombre de paiements réussis, "
            + "regroupés par jour, tous canaux confondus — vue du fondateur pour son contrôle mensuel "
            + "(indépendante du fait que les frais soient réglés par tranche ou en une fois)")
    @PreAuthorize("hasAuthority('paiement:read')")
    @GetMapping("/reports/daily")
    public PaymentUseCase.DailyCollectionsReport dailyCollectionsReport(
            @RequestParam java.time.LocalDateTime from,
            @RequestParam java.time.LocalDateTime to) {
        return useCase.dailyCollectionsReport(from, to);
    }
    @Operation(summary = "Supprimer un enregistrement de paiement")
    @PreAuthorize("hasAuthority('paiement:delete')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
