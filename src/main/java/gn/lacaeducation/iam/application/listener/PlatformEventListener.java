package gn.lacaeducation.iam.application.listener;

import gn.lacaeducation.iam.application.service.TenantProvisioningService;
import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.event.SubscriptionSuspendedEvent;
import gn.lacaeducation.platform.application.event.TenantActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSignupRequestedEvent;
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
        // Effet de bord après-commit : ne doit jamais faire échouer la requête de création du tenant
        // (le tenant est déjà persisté). Une erreur de provisioning est journalisée, pas propagée.
        try {
            provisioningService.provisionTenantAdmin(
                    event.tenantId(), event.schoolName(), event.adminEmail(), event.director(),
                    event.adminPassword(), event.temporaryPassword(), event.adminUsername());
        } catch (RuntimeException e) {
            log.error("Provisioning de l'admin échoué pour le tenant {} : {}", event.tenantId(), e.getMessage(), e);
        }
    }

    /** Self-service signup: create the unverified ADMIN_ECOLE with the chosen password. */
    @TransactionalEventListener
    public void onTenantSignupRequested(TenantSignupRequestedEvent event) {
        provisioningService.provisionUnverifiedAdmin(
                event.tenantId(), event.schoolName(), event.adminEmail(),
                event.director(), event.rawPassword(),
                event.adminFirstName(), event.adminLastName(), event.adminPhone(),
                event.adminUsername(), event.autoActivate());
    }

    /** School became usable (email verified or manual activation): activate the admin account. */
    @TransactionalEventListener
    public void onTenantActivated(TenantActivatedEvent event) {
        // Après-commit : ne jamais propager (le tenant/activation sont déjà persistés).
        try {
            provisioningService.markAdminVerified(event.tenantId(), event.adminEmail());
        } catch (RuntimeException e) {
            log.warn("markAdminVerified échoué pour le tenant {} : {}", event.tenantId(), e.getMessage());
        }
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
