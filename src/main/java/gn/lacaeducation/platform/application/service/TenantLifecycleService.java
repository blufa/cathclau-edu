package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.event.TenantActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantArchivedEvent;
import gn.lacaeducation.platform.application.event.TenantExpiredEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static gn.lacaeducation.platform.domain.TenantStatus.*;

/**
 * Single entry point for every school (tenant) state change (features/01-platform.md §1). Centralises
 * the state machine so transitions are validated, idempotent, audited and accompanied by the right
 * domain event. Illegal transitions are rejected as 422.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TenantLifecycleService {

    /** Allowed transitions; any (from → to) not listed here is illegal (422). */
    private static final Map<TenantStatus, Set<TenantStatus>> ALLOWED = Map.of(
            PENDING_VERIFICATION, Set.of(TRIAL, ACTIVE),
            TRIAL, Set.of(ACTIVE, EXPIRED, ARCHIVED),
            ACTIVE, Set.of(SUSPENDED, EXPIRED, ARCHIVED),
            SUSPENDED, Set.of(ACTIVE, ARCHIVED),
            EXPIRED, Set.of(ACTIVE, ARCHIVED),
            ARCHIVED, Set.of()
    );

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Moves a tenant to {@code target}. Idempotent: a no-op (and no event) when already in
     * {@code target}. Throws 422 for any disallowed transition.
     */
    public Tenant transition(Long tenantId, TenantStatus target, String reason) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
        return transition(tenant, target, reason);
    }

    public Tenant transition(Tenant tenant, TenantStatus target, String reason) {
        TenantStatus current = tenant.getStatus();
        if (current == target) {
            return tenant; // idempotent
        }
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessRuleException(
                    "Illegal tenant transition " + current + " → " + target);
        }

        tenant.setStatus(target);
        if ((target == TRIAL || target == ACTIVE) && tenant.getActivatedAt() == null) {
            tenant.setActivatedAt(LocalDateTime.now());
        }
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant {} transitioned {} → {} ({})", tenant.getId(), current, target, reason);

        publishFor(saved, current, target);
        return saved;
    }

    private void publishFor(Tenant t, TenantStatus from, TenantStatus to) {
        switch (to) {
            case TRIAL -> eventPublisher.publishEvent(new TenantActivatedEvent(
                    t.getId(), t.getSchoolName(), t.getEmail(), t.getDirector()));
            case ACTIVE -> {
                if (from == SUSPENDED) {
                    eventPublisher.publishEvent(new TenantReactivatedEvent(t.getId()));
                } else {
                    eventPublisher.publishEvent(new TenantActivatedEvent(
                            t.getId(), t.getSchoolName(), t.getEmail(), t.getDirector()));
                }
            }
            case SUSPENDED -> eventPublisher.publishEvent(new TenantSuspendedEvent(t.getId()));
            case EXPIRED -> eventPublisher.publishEvent(new TenantExpiredEvent(t.getId()));
            case ARCHIVED -> eventPublisher.publishEvent(new TenantArchivedEvent(t.getId()));
            default -> { /* no event */ }
        }
    }
}
