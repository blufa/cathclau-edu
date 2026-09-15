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
@Tag(name = "Payments", description = "Initiate and manage student fee payments via mobile money and card providers")
@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase useCase;
    private final PaymentTransactionUseCase transactionUseCase;
    private final ReceiptUseCase receiptUseCase;
    public record InitiateRequest(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type,
                                  String payerNumber, Long academicYearId) {}
    /** Initiation result: the payment plus the aggregator redirect URL (Djomy) when present. */
    public record InitiateResponse(Payment payment, String redirectUrl) {}
    @Operation(summary = "Initiate a new payment for a student invoice (returns redirectUrl for aggregator channels)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @PostMapping public ResponseEntity<InitiateResponse> initiate(@RequestBody InitiateRequest r, @AuthenticationPrincipal Jwt jwt) throws JsonProcessingException {
        return ResponseEntity.ok(doInitiate(r, currentUserId(jwt)));
    }

    public record GroupRequest(Long tenantId, Long studentId, PaymentChannel channel, String payerNumber,
                               String coverageLabel, Integer monthsCount, java.math.BigDecimal remainingAfter,
                               java.util.List<PaymentUseCase.InvoiceShare> shares, Long academicYearId,
                               /** Clé d'idempotence fournie par le client (mode hors-ligne, encaissement rejoué
                                *  après une coupure réseau) — voir {@link PaymentUseCase.GroupPaymentCommand}. */
                               String idempotencyKey) {}

    @Operation(summary = "Group payment of several months / the whole year — one payment, one receipt. "
            + "Desk channels are immediate; Djomy returns a redirectUrl for a single online transaction.")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
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

    @Operation(summary = "Confirm a payment by polling Djomy on demand (no webhook): on success, "
            + "confirms the payment, splits a group across its month-invoices and emits the receipt. Idempotent.")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY','PARENT','STUDENT')")
    @PostMapping("/{id}/confirm-djomy") public ResponseEntity<Payment> confirmViaDjomy(@PathVariable Long id) throws JsonProcessingException {
        return ResponseEntity.ok(useCase.confirmViaDjomy(id));
    }

    @Operation(summary = "Self-service online payment by a parent/student (online channels only — no cash)")
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
    @Operation(summary = "Query the live Djomy status of a payment (diagnostic: follow CREATED → PENDING → SUCCESS without waiting for the webhook)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/{id}/djomy-status") public ResponseEntity<PaymentUseCase.DjomyStatusResponse> djomyStatus(@PathVariable Long id) throws JsonProcessingException {
        return ResponseEntity.ok(useCase.checkDjomyStatus(id));
    }
    @Operation(summary = "Confirm a payment as successful")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/success") public ResponseEntity<Payment> confirmSuccess(@PathVariable Long id) { return ResponseEntity.ok(useCase.confirmSuccess(id)); }
    @Operation(summary = "Mark a payment as failed")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @PutMapping("/{id}/fail") public ResponseEntity<Payment> markFailed(@PathVariable Long id) { return ResponseEntity.ok(useCase.markFailed(id)); }
    @Operation(summary = "Mark a payment as refunded")
    @PreAuthorize("hasAuthority('paiement:refund')")
    @PutMapping("/{id}/refund") public ResponseEntity<Payment> markRefunded(@PathVariable Long id) { return ResponseEntity.ok(useCase.markRefunded(id)); }
    @Operation(summary = "Get a payment by ID")
    @GetMapping("/{id}") public ResponseEntity<Payment> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get a payment by its reference number")
    @GetMapping("/reference/{reference}") public ResponseEntity<Payment> findByReference(@PathVariable String reference) {
        return useCase.findByReference(reference).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "Get the provider transactions associated with a payment (§9.1)")
    @PreAuthorize("hasAuthority('transaction:read')")
    @GetMapping("/{id}/transactions") public List<PaymentTransaction> transactions(@PathVariable Long id) {
        return transactionUseCase.findByPaymentId(id);
    }
    @Operation(summary = "Get the receipt PDF URL for a successful payment (§9.1) — generated on demand if missing")
    @GetMapping("/{id}/receipt/pdf") public ResponseEntity<Map<String, String>> receiptPdf(@PathVariable Long id) {
        return receiptUseCase.findByPaymentId(id)
                .map(r -> ResponseEntity.ok(Map.of("url", receiptUseCase.pdfUrl(r.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Stream the receipt PDF bytes (inline) for a payment — independent of Cloudinary")
    @GetMapping("/{id}/receipt/pdf/download") public ResponseEntity<byte[]> receiptPdfDownload(@PathVariable Long id) {
        return receiptUseCase.findByPaymentId(id)
                .map(r -> ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recu-" + r.getId() + ".pdf\"")
                        .body(receiptUseCase.pdfBytes(r.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
    @Operation(summary = "List all payments for an invoice")
    @GetMapping("/invoice/{invoiceId}") public List<Payment> findByInvoice(@PathVariable Long invoiceId) { return useCase.findByInvoiceId(invoiceId); }
    @Operation(summary = "List all payments for a student (personnel)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT','SECRETARY')")
    @GetMapping("/student/{studentId}") public List<Payment> findByStudent(@PathVariable Long studentId) {
        return useCase.findByStudentId(studentId);
    }
    @Operation(summary = "List all payments for a tenant — pass academicYearId to scope to one school year (recommended: an unscoped call mixes every year ever created)")
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
    @Operation(summary = "Delete a payment record")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { useCase.delete(id); return ResponseEntity.noContent().build(); }
}
