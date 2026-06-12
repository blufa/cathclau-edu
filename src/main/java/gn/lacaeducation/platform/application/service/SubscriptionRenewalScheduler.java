package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.event.SubscriptionExpiringSoonEvent;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily job implementing CLAUDE.md step 7: at end_date, auto-renew or enter grace period then suspend.
 * Grace period: 3 days after end_date before suspension. Also emits a J-7 expiring-soon reminder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalScheduler {

    private static final int GRACE_PERIOD_DAYS = 3;
    private static final int EXPIRY_REMINDER_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final AbonnementService abonnementService;
    private final ApplicationEventPublisher eventPublisher;

    /** J-7 reminder: notify the admin that the subscription is about to expire (AbonnementBientotExpire). */
    @Scheduled(cron = "0 30 1 * * *")
    @Transactional(readOnly = true)
    public void notifyExpiringSoon() {
        LocalDate target = LocalDate.now().plusDays(EXPIRY_REMINDER_DAYS);
        for (Subscription sub : subscriptionRepository.findActiveExpiringOn(target)) {
            eventPublisher.publishEvent(new SubscriptionExpiringSoonEvent(
                    sub.getTenantId(), sub.getId(), sub.getEndDate()));
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // runs at 01:00 every day
    @Transactional
    public void processExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> expired = subscriptionRepository.findActiveExpiredBefore(today);

        for (Subscription sub : expired) {
            try {
                if (Boolean.TRUE.equals(sub.getAutoRenewal())) {
                    renewSubscription(sub, today);
                } else if (today.isAfter(sub.getEndDate().plusDays(GRACE_PERIOD_DAYS))) {
                    suspendTenant(sub);
                }
            } catch (Exception e) {
                log.error("Failed to process expired subscription id={}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    private void renewSubscription(Subscription sub, LocalDate today) {
        log.info("Auto-renewing subscription id={} for tenant id={}", sub.getId(), sub.getTenantId());
        // Calculate new period based on previous duration
        long periodDays = sub.getStartDate().until(sub.getEndDate()).getDays();
        boolean isAnnual = periodDays > 32;

        LocalDate newStart = sub.getEndDate();
        LocalDate newEnd = isAnnual ? newStart.plusYears(1) : newStart.plusMonths(1);

        sub.setStartDate(newStart);
        sub.setEndDate(newEnd);
        sub.transitionTo(SubscriptionStatus.PENDING);
        subscriptionRepository.save(sub);

        var billingPeriod = isAnnual
                ? gn.lacaeducation.platform.domain.BillingPeriod.ANNUAL
                : gn.lacaeducation.platform.domain.BillingPeriod.MONTHLY;

        abonnementService.generate(sub.getId(), billingPeriod, newStart, newEnd);
        log.info("Renewal invoice generated for subscription id={}", sub.getId());
    }

    private void suspendTenant(Subscription sub) {
        log.info("Suspending tenant id={} — grace period expired for subscription id={}", sub.getTenantId(), sub.getId());
        sub.transitionTo(SubscriptionStatus.SUSPENDED);
        subscriptionRepository.save(sub);

        tenantRepository.findById(sub.getTenantId()).ifPresent(tenant -> {
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepository.save(tenant);
        });
    }
}
