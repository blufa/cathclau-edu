package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Tenant create(CreateTenantCommand c) {
        return tenantRepository.save(Tenant.builder()
                .schoolName(c.schoolName()).email(c.email()).phone(c.phone())
                .address(c.address()).city(c.city()).country(c.country())
                .director(c.director()).status(TenantStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly = true)
    public Tenant findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Override @Transactional(readOnly = true)
    public List<Tenant> findAll() { return tenantRepository.findAll(); }

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
            s.setStatus(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(s);
        });
        eventPublisher.publishEvent(new TenantSuspendedEvent(id));
    }

    @Override
    public void activate(Long id) {
        Tenant t = findById(id);
        t.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(t);
    }

    @Override
    public void delete(Long id) {
        Tenant t = findById(id);
        t.setStatus(TenantStatus.ARCHIVED);
        tenantRepository.save(t);
    }
}
