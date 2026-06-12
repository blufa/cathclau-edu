package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import gn.lacaeducation.platform.StudentCountPort;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional @RequiredArgsConstructor
public class TenantService implements CreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StudentCountPort studentCountPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Tenant create(CreateTenantCommand c) {
        Tenant saved = tenantRepository.save(Tenant.builder()
                .schoolName(c.schoolName()).email(c.email()).phone(c.phone())
                .address(c.address()).city(c.city()).country(c.country())
                .director(c.director()).status(TenantStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build());
        // TenantCree → iam provisions the ADMIN_ECOLE user + roles; communication sends welcome email.
        eventPublisher.publishEvent(new TenantCreatedEvent(
                saved.getId(), saved.getSchoolName(), saved.getEmail(), saved.getDirector()));
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Tenant findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    @Override @Transactional(readOnly = true)
    public TenantDetail getDetail(Long id) {
        Tenant tenant = findById(id);
        Subscription sub = subscriptionRepository.findActiveByTenantId(id).orElse(null);
        Plan plan = sub == null ? null : planRepository.findById(sub.getPlanId()).orElse(null);
        long consumed = studentCountPort.countByTenant(id);
        Integer limit = plan == null ? null : plan.getStudentLimit();
        return new TenantDetail(tenant, sub, plan, consumed, limit);
    }

    @Override @Transactional(readOnly = true)
    public List<Tenant> findAll(String status, String city) {
        return tenantRepository.findAll().stream()
                .filter(t -> status == null || (t.getStatus() != null && t.getStatus().name().equalsIgnoreCase(status)))
                .filter(t -> city == null || (t.getCity() != null && t.getCity().equalsIgnoreCase(city)))
                .toList();
    }

    @Override
    public Tenant update(Long id, UpdateTenantCommand c) {
        Tenant t = findById(id);
        t.setSchoolName(c.schoolName()); t.setLogo(c.logo()); t.setBrandColor(c.brandColor());
        t.setAddress(c.address()); t.setCity(c.city()); t.setCountry(c.country());
        t.setPhone(c.phone()); t.setEmail(c.email()); t.setWebsite(c.website()); t.setDirector(c.director());
        return tenantRepository.save(t);
    }

    @Override
    public void suspend(Long id) {
        Tenant t = findById(id);
        t.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(t);
        subscriptionRepository.findActiveByTenantId(id).ifPresent(s -> {
            s.transitionTo(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(s);
        });
        eventPublisher.publishEvent(new TenantSuspendedEvent(id));
    }

    @Override
    public void activate(Long id) {
        Tenant t = findById(id);
        t.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(t);
        eventPublisher.publishEvent(new TenantReactivatedEvent(id));
    }

    @Override
    public void delete(Long id) {
        Tenant t = findById(id);
        t.setStatus(TenantStatus.ARCHIVED);
        tenantRepository.save(t);
    }
}
