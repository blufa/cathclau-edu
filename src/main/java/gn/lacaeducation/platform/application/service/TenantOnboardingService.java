package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.event.TenantActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantOnboardedEvent;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Automatic onboarding once a school becomes usable (features/01-platform.md §2). Runs after the
 * activating transaction commits and is idempotent: {@code Tenant.onboardedAt} guards against
 * re-running on event replay. Publishes {@link TenantOnboardedEvent} so billing-subscription creates
 * the TRIAL subscription and communication sends the welcome email + director guide.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingService {

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTenantActivated(TenantActivatedEvent event) {
        onboard(event.tenantId());
    }

    /** Idempotent onboarding step; safe to call repeatedly. */
    public void onboard(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getOnboardedAt() != null) {
            return; // unknown or already onboarded
        }
        tenant.setOnboardedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        log.info("Onboarded tenant {} ({})", tenantId, tenant.getSchoolName());

        eventPublisher.publishEvent(new TenantOnboardedEvent(
                tenant.getId(), tenant.getSchoolName(), tenant.getEmail()));
    }
}
