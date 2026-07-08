package gn.lacaeducation.platform.application.listener;

import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.service.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Provisions a tenant's effective modules from its paid plan when a subscription becomes active
 * (features/SCHOOL_back_complements.md §M). Free trials get full module access dynamically while the
 * trial is unexpired (see {@code ModuleService#getActiveModules}), so no provisioning is needed there.
 * Runs after commit in a new transaction so writes persist independently of the originating one.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModuleProvisioningListener {

    private final SubscriptionRepository subscriptionRepository;
    private final ModuleService moduleService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        subscriptionRepository.findById(event.subscriptionId()).ifPresent(sub -> {
            moduleService.provisionFromPlan(event.tenantId(), sub.getPlanId());
            log.info("Provisioned modules for tenant {} from plan {}", event.tenantId(), sub.getPlanId());
        });
    }
}
