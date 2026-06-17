package gn.lacaeducation.payments.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.DjomyCallbackHandler;
import gn.lacaeducation.payments.PaymentCallbackHandler;
import gn.lacaeducation.payments.application.port.out.DjomyVerificationPort;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Fulfilment of Djomy callbacks (§10.1). Never validates on the webhook alone: payment.success is
 * confirmed server-side via verify_payment before driving the payment to SUCCEEDED.
 */
@Service
@Transactional
@RequiredArgsConstructor
class DjomyCallbackService implements DjomyCallbackHandler {

    private final DjomyVerificationPort verifier;
    private final PaymentTransactionRepository transactionRepo;
    private final PaymentCallbackHandler callbackHandler;

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        return verifier.verifyWebhookSignature(rawBody, signatureHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> resolveTransactionId(String externalTransactionId) {
        return transactionRepo.findByExternalTransactionId(externalTransactionId).map(PaymentTransaction::getId);
    }

    @Override
    public boolean confirmSuccess(String externalTransactionId) throws JsonProcessingException {
        Optional<PaymentTransaction> tx = transactionRepo.findByExternalTransactionId(externalTransactionId);
        if (tx.isEmpty()) {
            return false;
        }
        // Server-side confirmation required before validating anything.
        if (!verifier.isConfirmedSuccess(externalTransactionId)) {
            return false;
        }
        callbackHandler.applyCallback(tx.get().getId(), true, "SUCCESS", "djomy verify_payment");
        return true;
    }

    @Override
    public void markFailed(String externalTransactionId) {
        transactionRepo.findByExternalTransactionId(externalTransactionId)
                .ifPresent(tx -> callbackHandler.applyCallback(tx.getId(), false, "FAILED", "djomy payment.failed"));
    }
}
