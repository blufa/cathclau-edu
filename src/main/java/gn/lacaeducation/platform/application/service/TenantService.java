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

@Service
@Transactional
@RequiredArgsConstructor
public class TenantService implements CreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Tenant create(CreateTenantCommand command) {
        Tenant tenant = Tenant.builder()
                .schoolName(command.schoolName())
                .email(command.email())
                .phone(command.phone())
                .address(command.address())
                .city(command.city())
                .country(command.country())
                .director(command.director())
                .status(TenantStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    @Override
    public Tenant update(Long id, UpdateTenantCommand command) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenant.setSchoolName(command.schoolName());
        tenant.setLogo(command.logo());
        tenant.setBrandColor(command.brandColor());
        tenant.setAddress(command.address());
        tenant.setCity(command.city());
        tenant.setCountry(command.country());
        tenant.setPhone(command.phone());
        tenant.setEmail(command.email());
        tenant.setWebsite(command.website());
        tenant.setDirector(command.director());
        return tenantRepository.save(tenant);
    }

    @Override
    public void suspend(Long tenantId) {
        subscriptionRepository.findActiveByTenantId(tenantId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(subscription);
        });
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepository.save(tenant);
        });
        eventPublisher.publishEvent(new TenantSuspendedEvent(tenantId));
    }
}
