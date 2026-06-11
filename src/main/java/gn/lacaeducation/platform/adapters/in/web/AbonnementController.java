package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.ChangePlanRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.ConfirmPaymentRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.InitiatePaymentRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.InitiateSubscriptionRequest;
import gn.lacaeducation.platform.adapters.in.web.dto.SubscriptionResponse;
import gn.lacaeducation.platform.application.port.in.PaymentUseCase;
import gn.lacaeducation.platform.application.port.in.PlanChangeUseCase;
import gn.lacaeducation.platform.application.port.in.SubscriptionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class AbonnementController {

    private final SubscriptionUseCase subscriptionUseCase;
    private final PaymentUseCase paymentUseCase;
    private final PlanChangeUseCase planChangeUseCase;

    @PostMapping("/tenants/{tenantId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse initiate(@PathVariable Long tenantId,
                                         @Valid @RequestBody InitiateSubscriptionRequest request) {
        return SubscriptionResponse.from(subscriptionUseCase.initiate(
            new SubscriptionUseCase.InitiateSubscriptionCommand(
                tenantId, request.planId(), request.billingPeriod(), request.autoRenewal()
            )
        ));
    }

    @GetMapping("/tenants/{tenantId}/subscriptions/active")
    public ResponseEntity<SubscriptionResponse> findActive(@PathVariable Long tenantId) {
        return subscriptionUseCase.findActive(tenantId)
                .map(SubscriptionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subscriptions/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public void initiatePayment(@PathVariable Long id,
                                @Valid @RequestBody InitiatePaymentRequest request) {
        paymentUseCase.initiate(
            new PaymentUseCase.InitiatePaymentCommand(
                request.invoiceId(), request.amount(), request.channel(), request.paymentType()
            )
        );
    }

    @PutMapping("/payments/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable Long id,
                               @Valid @RequestBody ConfirmPaymentRequest request) {
        paymentUseCase.confirm(id, request.externalTransactionId());
    }

    @PostMapping("/subscriptions/{id}/change-plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePlan(@PathVariable Long id,
                           @Valid @RequestBody ChangePlanRequest request) {
        planChangeUseCase.changePlan(
            new PlanChangeUseCase.ChangePlanCommand(
                id, request.newPlanId(), request.newBillingPeriod()
            )
        );
    }
}
