package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.DjomyGatewayApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementation of the published {@link DjomyGatewayApi}: thin facade over {@link DjomyClient} so
 * other modules (platform subscription payments) reuse the same Djomy integration without touching
 * the HTTP client or the credentials.
 */
@Service
@RequiredArgsConstructor
class DjomyGatewayApiImpl implements DjomyGatewayApi {

    private final DjomyClient client;

    @Override
    public GatewayResult createGatewayPayment(String payerNumber, BigDecimal amount, String reference, String description) throws JsonProcessingException {
        DjomyClient.GatewayResult r = client.createPaymentGateway(payerNumber, amount, reference, description);
        return new GatewayResult(r.transactionId(), r.redirectUrl());
    }

    @Override
    public DjomyStatus verify(String externalTransactionId) throws JsonProcessingException {
        DjomyClient.Verification v = client.verifyPayment(externalTransactionId);
        return new DjomyStatus(v.status(), v.receivedAmount(), v.currency(), v.isSuccess());
    }

    @Override
    public boolean isConfirmedSuccess(String externalTransactionId) throws JsonProcessingException{
        return client.verifyPayment(externalTransactionId).isSuccess();
    }
}
