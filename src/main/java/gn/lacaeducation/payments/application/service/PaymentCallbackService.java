package gn.lacaeducation.payments.application.service;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.PaymentCallbackHandler;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies provider callbacks (§10.1): updates the TransactionPaiement with the raw response,
 * then drives the matching Paiement to SUCCEEDED or FAILED (emitting PaiementReussi / PaiementEchoue
 * via {@link PaymentService}).
 */
@Service @Transactional @RequiredArgsConstructor
class PaymentCallbackService implements PaymentCallbackHandler {

    private final PaymentTransactionRepository transactionRepo;
    private final PaymentUseCase paymentUseCase;

    @Override
    public void applyCallback(Long transactionId, boolean success, String providerStatus, String rawPayload) {
        PaymentTransaction tx = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", transactionId));
        tx.setProviderStatus(providerStatus);
        tx.setResponsePayload(rawPayload);
        transactionRepo.save(tx);
        if (success) {
            paymentUseCase.confirmSuccess(tx.getPaymentId());
        } else {
            paymentUseCase.markFailed(tx.getPaymentId());
        }
    }
}
