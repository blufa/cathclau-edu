package gn.lacaeducation.iam.application.listener;

import gn.lacaeducation.iam.application.service.TenantProvisioningService;
import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.event.SubscriptionSuspendedEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumes platform events (§18 event contract): provisions the admin/roles on tenant creation and
 * blocks tenant access on suspension by revoking active sessions. Re-activation needs no positive
 * action — login is gated by {@code TenantInfoProvider.isTenantActive}, so access resumes once the
 * tenant is active again.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformEventListener {

    private final TenantProvisioningService provisioningService;

    @TransactionalEventListener
    public void onTenantCreated(TenantCreatedEvent event) {
        provisioningService.provisionTenantAdmin(
                event.tenantId(), event.schoolName(), event.adminEmail(), event.director());
    }

    @TransactionalEventListener
    public void onSubscriptionSuspended(SubscriptionSuspendedEvent event) {
        provisioningService.revokeTenantSessions(event.tenantId());
    }

    @TransactionalEventListener
    public void onTenantSuspended(TenantSuspendedEvent event) {
        provisioningService.revokeTenantSessions(event.tenantId());
    }

    @TransactionalEventListener
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("Subscription activated for tenant {}: access unblocked", event.tenantId());
    }

    @TransactionalEventListener
    public void onTenantReactivated(TenantReactivatedEvent event) {
        log.info("Tenant {} reactivated: access unblocked", event.tenantId());
    }
}
