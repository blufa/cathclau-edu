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

@Tag(name = "Subscriptions", description = "Gestion du cycle de vie des abonnements SaaS")
@RestController
@RequestMapping("/api/v1/platform/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;
    private final PaymentUseCase paymentUseCase;
    private final PlanChangeUseCase planChangeUseCase;
    private final InvoiceUseCase invoiceUseCase;

    @Operation(summary = "Créer un nouvel abonnement pour un établissement")
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

    @Operation(summary = "Auto-service : l'école courante souscrit à un plan payant "
            + "(crée un abonnement EN_ATTENTE + sa première facture ; payer via Djomy pour activer)")
    @PostMapping("/subscribe")
    @PreAuthorize("hasAuthority('abonnement:manage')")
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SelfSubscribeRequest req) {
        var sub = subscriptionUseCase.initiate(new SubscriptionUseCase.InitiateSubscriptionCommand(
                TenantContext.get(), req.planId(), req.billingPeriod(), req.autoRenewal()));
        return ResponseEntity.created(URI.create("/api/v1/platform/subscriptions/" + sub.getId()))
                .body(SubscriptionResponse.from(sub));
    }

    @Operation(summary = "Lister tous les abonnements (filtres : statut, planId)")
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

    @Operation(summary = "Consulter un abonnement par son identifiant")
    @GetMapping("/{id}")
    public SubscriptionResponse findById(@PathVariable Long id) {
        return SubscriptionResponse.from(subscriptionUseCase.findById(id));
    }

    // Pas de @PreAuthorize dédié : appelé par SubscriptionProvider (frontend) pour TOUS les
    // utilisateurs connectés à chaque chargement de page (statut d'abonnement affiché dans le
    // shell de l'appli), quel que soit leur rôle — authentification seule suffit, comme pour un
    // endpoint /me. Restreindre à hasAuthority('abonnement:read') casse ce statut pour tout rôle
    // personnalisé n'ayant pas explicitement cette permission (déjà vécu avec un rôle Comptable).
    @Operation(summary = "Consulter l'abonnement actif de l'établissement courant")
    @GetMapping("/active")
    public ResponseEntity<SubscriptionResponse> findActive() {
        return subscriptionUseCase.findActiveForCurrentTenant()
                .map(SubscriptionResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Consulter le dernier abonnement de l'établissement courant (y compris EN_ATTENTE de paiement)")
    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> findCurrent() {
        return subscriptionUseCase.findLatestForCurrentTenant()
                .map(SubscriptionResponse::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Suspendre un abonnement")
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long id) { subscriptionUseCase.suspend(id); }

    @Operation(summary = "Résilier un abonnement")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) { subscriptionUseCase.cancel(id); }

    @Operation(summary = "Activer/désactiver le renouvellement automatique")
    @PatchMapping("/{id}/auto-renewal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAutoRenewal(@PathVariable Long id, @RequestParam boolean enabled) {
        subscriptionUseCase.setAutoRenewal(id, enabled);
    }

    @Operation(summary = "Changer de plan (upgrade ou downgrade)")
    @PostMapping("/{id}/change-plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePlan(@PathVariable Long id, @Valid @RequestBody ChangePlanRequest req) {
        planChangeUseCase.changePlan(new PlanChangeUseCase.ChangePlanCommand(
                id, req.newPlanId(), req.newBillingPeriod()));
    }

    @Operation(summary = "Initier un paiement pour une facture d'abonnement "
            + "(retourne le paiement créé ; redirectUrl renseigné pour les canaux agrégateurs comme Djomy)")
    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPaymentResponse initiatePayment(@PathVariable Long id, @Valid @RequestBody InitiatePaymentRequest req) throws JsonProcessingException {
        return SubscriptionPaymentResponse.from(paymentUseCase.initiate(new PaymentUseCase.InitiatePaymentCommand(
                req.invoiceId(), req.amount(), req.channel(), req.paymentType(), req.payerNumber())));
    }

    @Operation(summary = "Confirmer un paiement (callback / manuel)")
    @PutMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable Long paymentId, @Valid @RequestBody ConfirmPaymentRequest req) {
        paymentUseCase.confirm(paymentId, req.externalTransactionId());
    }

    @Operation(summary = "Marquer un paiement comme échoué")
    @PutMapping("/payments/{paymentId}/fail")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void failPayment(@PathVariable Long paymentId) {
        paymentUseCase.fail(paymentId);
    }

    @Operation(summary = "Interroger le statut Djomy en direct d'un paiement "
            + "(diagnostic : suivre CREATED → PENDING → SUCCESS sans attendre le webhook)")
    @GetMapping("/payments/{paymentId}/djomy-status")
    @PreAuthorize("hasAuthority('abonnement:read')")
    public PaymentUseCase.DjomyStatusResponse djomyStatus(@PathVariable Long paymentId) throws JsonProcessingException {
        return paymentUseCase.checkDjomyStatus(paymentId);
    }

    @Operation(summary = "Confirmer et activer l'abonnement en interrogeant Djomy à la demande "
            + "(active sans attendre le webhook ; réussit uniquement si Djomy renvoie SUCCESS)")
    @PostMapping("/payments/{paymentId}/confirm-djomy")
    @PreAuthorize("hasAuthority('abonnement:manage')")
    public SubscriptionPaymentResponse confirmViaDjomy(@PathVariable Long paymentId) throws JsonProcessingException {
        return SubscriptionPaymentResponse.from(paymentUseCase.confirmViaDjomy(paymentId));
    }

    @Operation(summary = "Lister toutes les factures d'un abonnement")
    @GetMapping("/{id}/invoices")
    public List<SubscriptionInvoiceResponse> findInvoices(@PathVariable Long id) {
        return invoiceUseCase.findAllBySubscription(id)
                .stream().map(SubscriptionInvoiceResponse::from).toList();
    }

    @Operation(summary = "Consulter l'historique des changements de plan d'un abonnement")
    @GetMapping("/{id}/plan-changes")
    public List<PlanChangeResponse> planChanges(@PathVariable Long id) {
        return planChangeUseCase.findBySubscription(id).stream()
                .map(PlanChangeResponse::from).toList();
    }

    @Operation(summary = "Consulter le statut d'un paiement d'abonnement")
    @GetMapping("/payments/{paymentId}")
    public SubscriptionPaymentResponse findPayment(@PathVariable Long paymentId) {
        return SubscriptionPaymentResponse.from(paymentUseCase.findPaymentById(paymentId));
    }

    @Operation(summary = "Factures de l'établissement courant")
    @GetMapping("/active/invoices")
    @PreAuthorize("hasAuthority('abonnement:read')")
    public List<SubscriptionInvoiceResponse> myInvoices() {
        Long tenantId = gn.lacaeducation.common.TenantContext.get();
        if (tenantId == null) return List.of();
        return invoiceUseCase.findInvoicesByTenant(tenantId)
                .stream().map(SubscriptionInvoiceResponse::from).toList();
    }

    @Operation(summary = "Télécharger le PDF d'une facture d'abonnement (URL)")
    @GetMapping("/invoices/{invoiceId}/pdf")
    public Map<String, String> invoicePdf(@PathVariable Long invoiceId) {
        return Map.of("pdfUrl", invoiceUseCase.generateInvoicePdf(invoiceId));
    }

    @Operation(summary = "Télécharger le PDF d'un reçu de paiement d'abonnement (URL)")
    @GetMapping("/receipts/{receiptId}/pdf")
    public Map<String, String> receiptPdf(@PathVariable Long receiptId) {
        return Map.of("pdfUrl", paymentUseCase.generateReceiptPdf(receiptId));
    }
}
