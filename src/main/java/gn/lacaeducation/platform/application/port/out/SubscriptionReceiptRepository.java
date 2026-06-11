package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.SubscriptionReceipt;

import java.util.Optional;

public interface SubscriptionReceiptRepository {
    SubscriptionReceipt save(SubscriptionReceipt receipt);
    Optional<SubscriptionReceipt> findById(Long id);
    Optional<SubscriptionReceipt> findByPaymentId(Long paymentId);
}
