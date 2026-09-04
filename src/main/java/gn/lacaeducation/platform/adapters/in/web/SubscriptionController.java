package gn.lacaeducation.platform.adapters.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.platform.adapters.in.web.dto.*;
import gn.lacaeducation.platform.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.platform.domain.BillingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Subscriptions", description = "SaaS subscription lifecycle management")
@RestController
@RequestMapping("/api/v1/platform/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;
    private final PaymentUseCase paymentUseCase;
    private final PlanChangeUseCase planChangeUseCase;
    private final InvoiceUseCase invoiceUseCase;

    @Operation(summary = "Create a new subscription for a tenant")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody InitiateSubscriptionRequest req) {
        var sub = subscriptionUseCase.initiate(new SubscriptionUseCase.InitiateSubscriptionCommand(
                req.tenantId(), req.planId(), req.billingPeriod(), req.autoRenewal()));
        return ResponseEntity.created(URI.create("/api/v1/platform/subscriptions/" + sub.getId()))
                .body(SubscriptionResponse.from(sub));
    }

    public record SelfSubscribeRequest(@jakarta.validation.constraints.NotNull Long planId,
                                       @jakarta.validation.constraints.NotNull BillingPeriod billingPeriod,
                                       Boolean autoRenewal) {}

    @Operation(summary = "Self-service: the current school subscribes to a paid plan "
            + "(creates an EN_ATTENTE subscription + its first invoice; pay via Djomy to activate)")
    @PostMapping("/subscribe")
    @PreAuthorize("hasAuthority('abonnement:manage')")
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SelfSubscribeRequest req) {
        var sub = subscriptionUseCase.initiate(new SubscriptionUseCase.InitiateSubscriptionCommand(
                TenantContext.get(), req.planId(), req.billingPeriod(), req.autoRenewal()));
        return ResponseEntity.created(URI.create("/api/v1/platform/subscriptions/" + sub.getId()))
                .body(SubscriptionResponse.from(sub));
    }

    @Operation(summary = "List all subscriptions (filters: status, planId)")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<SubscriptionResponse> findAll(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Long planId,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        List<SubscriptionResponse> list = subscriptionUseCase.findAll().stream()
                .filter(s -> status == null || (s.getStatus() != null && s.getStatus().name().equalsIgnoreCase(status)))
                .filter(s -> planId == null || planId.equals(s.getPlanId()))
                .map(SubscriptionResponse::from).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get subscription by ID")
    @GetMapping("/{id}")
    public SubscriptionResponse findById(@PathVariable Long id) {
        return SubscriptionResponse.from(subscriptionUseCase.findById(id));
    }

    // Pas de @PreAuthorize dédié : appelé par SubscriptionProvider (frontend) pour TOUS les
    // utilisateurs connectés à chaque chargement de page (statut d'abonnement affiché dans le
    // shell de l'appli), quel que soit leur rôle — authentification seule suffit, comme pour un
    // endpoint /me. Restreindre à hasAuthority('abonnement:read') casse ce statut pour tout rôle
    // personnalisé n'ayant pas explicitement cette permission (déjà vécu avec un rôle Comptable).
    @Operation(summary = "Get the active subscription of the current tenant")
    @GetMapping("/active")
    public ResponseEntity<SubscriptionResponse> findActive() {
        return subscriptionUseCase.findActiveForCurrentTenant()
                .map(SubscriptionResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the current tenant's latest subscription (incl. PENDING awaiting payment)")
    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> findCurrent() {
        return subscriptionUseCase.findLatestForCurrentTenant()
                .map(SubscriptionResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Suspend a subscription")
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long id) { subscriptionUseCase.suspend(id); }

    @Operation(summary = "Cancel a subscription")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) { subscriptionUseCase.cancel(id); }

    @Operation(summary = "Toggle auto-renewal")
    @PatchMapping("/{id}/auto-renewal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAutoRenewal(@PathVariable Long id, @RequestParam boolean enabled) {
        subscriptionUseCase.setAutoRenewal(id, enabled);
    }

    @Operation(summary = "Upgrade or downgrade plan")
    @PostMapping("/{id}/change-plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePlan(@PathVariable Long id, @Valid @RequestBody ChangePlanRequest req) {
        planChangeUseCase.changePlan(new PlanChangeUseCase.ChangePlanCommand(
                id, req.newPlanId(), req.newBillingPeriod()));
    }

    @Operation(summary = "Initiate a payment for a subscription invoice "
            + "(returns the created payment; redirectUrl is set for aggregator channels like Djomy)")
    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPaymentResponse initiatePayment(@PathVariable Long id, @Valid @RequestBody InitiatePaymentRequest req) throws JsonProcessingException {
        return SubscriptionPaymentResponse.from(paymentUseCase.initiate(new PaymentUseCase.InitiatePaymentCommand(
                req.invoiceId(), req.amount(), req.channel(), req.paymentType(), req.payerNumber())));
    }

    @Operation(summary = "Confirm a payment (callback / manual)")
    @PutMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable Long paymentId, @Valid @RequestBody ConfirmPaymentRequest req) {
        paymentUseCase.confirm(paymentId, req.externalTransactionId());
    }

    @Operation(summary = "Mark a payment as failed")
    @PutMapping("/payments/{paymentId}/fail")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void failPayment(@PathVariable Long paymentId) {
        paymentUseCase.fail(paymentId);
    }

    @Operation(summary = "Query the live Djomy status of a payment "
            + "(diagnostic: follow CREATED → PENDING → SUCCESS without waiting for the webhook)")
    @GetMapping("/payments/{paymentId}/djomy-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN')")
    public PaymentUseCase.DjomyStatusResponse djomyStatus(@PathVariable Long paymentId) throws JsonProcessingException {
        return paymentUseCase.checkDjomyStatus(paymentId);
    }

    @Operation(summary = "Confirm & activate the subscription by polling Djomy on demand "
            + "(activates without waiting for the webhook; only succeeds if Djomy reports SUCCESS)")
    @PostMapping("/payments/{paymentId}/confirm-djomy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN')")
    public SubscriptionPaymentResponse confirmViaDjomy(@PathVariable Long paymentId) throws JsonProcessingException {
        return SubscriptionPaymentResponse.from(paymentUseCase.confirmViaDjomy(paymentId));
    }

    @Operation(summary = "Get all invoices for a subscription")
    @GetMapping("/{id}/invoices")
    public List<SubscriptionInvoiceResponse> findInvoices(@PathVariable Long id) {
        return invoiceUseCase.findAllBySubscription(id)
                .stream().map(SubscriptionInvoiceResponse::from).toList();
    }

    @Operation(summary = "Get the plan-change history of a subscription")
    @GetMapping("/{id}/plan-changes")
    public List<PlanChangeResponse> planChanges(@PathVariable Long id) {
        return planChangeUseCase.findBySubscription(id).stream()
                .map(PlanChangeResponse::from).toList();
    }

    @Operation(summary = "Get a subscription payment status")
    @GetMapping("/payments/{paymentId}")
    public SubscriptionPaymentResponse findPayment(@PathVariable Long paymentId) {
        return SubscriptionPaymentResponse.from(paymentUseCase.findPaymentById(paymentId));
    }

    @Operation(summary = "Invoices of the current tenant")
    @GetMapping("/active/invoices")
    @PreAuthorize("hasAuthority('abonnement:read')")
    public List<SubscriptionInvoiceResponse> myInvoices() {
        Long tenantId = gn.lacaeducation.common.TenantContext.get();
        if (tenantId == null) return List.of();
        return invoiceUseCase.findInvoicesByTenant(tenantId)
                .stream().map(SubscriptionInvoiceResponse::from).toList();
    }

    @Operation(summary = "Download a subscription invoice PDF (URL)")
    @GetMapping("/invoices/{invoiceId}/pdf")
    public Map<String, String> invoicePdf(@PathVariable Long invoiceId) {
        return Map.of("pdfUrl", invoiceUseCase.generateInvoicePdf(invoiceId));
    }

    @Operation(summary = "Download a subscription payment receipt PDF (URL)")
    @GetMapping("/receipts/{receiptId}/pdf")
    public Map<String, String> receiptPdf(@PathVariable Long receiptId) {
        return Map.of("pdfUrl", paymentUseCase.generateReceiptPdf(receiptId));
    }
}
