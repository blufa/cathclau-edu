package gn.lacaeducation.platform.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.DjomyGatewayApi;
import gn.lacaeducation.platform.SubscriptionDjomyCallback;
import gn.lacaeducation.platform.application.port.in.PaymentUseCase;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fulfilment of Djomy callbacks for subscription payments. Never trusts the webhook alone:
 * payment.success is confirmed via verify_payment before the subscription is activated.
 */
@Service
@Transactional
@RequiredArgsConstructor
class SubscriptionDjomyCallbackService implements SubscriptionDjomyCallback {

    private final SubscriptionPaymentRepository paymentRepository;
    private final PaymentUseCase paymentUseCase;
    private final DjomyGatewayApi djomyGatewayApi;

    @Override
    @Transactional(readOnly = true)
    public boolean handles(String externalTransactionId) {
        return paymentRepository.findByExternalTransactionId(externalTransactionId).isPresent();
    }

    @Override
    public boolean confirmSuccess(String externalTransactionId) throws JsonProcessingException {
        Optional<SubscriptionPayment> payment = paymentRepository.findByExternalTransactionId(externalTransactionId);
        if (payment.isEmpty()) {
            return false;
        }
        if (!djomyGatewayApi.isConfirmedSuccess(externalTransactionId)) {
            return false;
        }
        paymentUseCase.confirm(payment.get().getId(), externalTransactionId);
        return true;
    }

    @Override
    public void markFailed(String externalTransactionId) {
        paymentRepository.findByExternalTransactionId(externalTransactionId)
                .ifPresent(p -> paymentUseCase.fail(p.getId()));
    }
}
