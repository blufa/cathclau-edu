package gn.lacaeducation.platform.application.port.in;
import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.Subscription;
import java.util.List;
import java.util.Optional;
public interface SubscriptionUseCase {
    Subscription initiate(InitiateSubscriptionCommand command);
    Subscription findById(Long id);
    Optional<Subscription> findActive(Long tenantId);
    Optional<Subscription> findActiveForCurrentTenant();
    List<Subscription> findAll();
    void suspend(Long id);
    void cancel(Long id);
    void setAutoRenewal(Long id, boolean enabled);

    record InitiateSubscriptionCommand(Long tenantId, Long planId, BillingPeriod billingPeriod, Boolean autoRenewal) {}
}
