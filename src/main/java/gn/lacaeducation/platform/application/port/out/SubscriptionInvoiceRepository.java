package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.SubscriptionInvoice;

import java.util.Optional;

public interface SubscriptionInvoiceRepository {
    SubscriptionInvoice save(SubscriptionInvoice invoice);
    Optional<SubscriptionInvoice> findById(Long id);
    Optional<SubscriptionInvoice> findLatestBySubscriptionId(Long subscriptionId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
