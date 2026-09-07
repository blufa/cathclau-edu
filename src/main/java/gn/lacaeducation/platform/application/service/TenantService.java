package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TemporaryPasswords;
import gn.lacaeducation.platform.application.event.TenantActivatedEvent;
import gn.lacaeducation.platform.application.event.TenantAdminCredentialsIssuedEvent;
import gn.lacaeducation.platform.application.event.TenantBrandingUpdatedEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.port.in.CreateTenantUseCase;
import gn.lacaeducation.platform.StudentCountPort;
import gn.lacaeducation.platform.TenantEstablishmentTypeService;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service @Transactional @RequiredArgsConstructor
public class TenantService implements CreateTenantUseCase, TenantEstablishmentTypeService {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StudentCountPort studentCountPort;
    private final TenantLifecycleService lifecycleService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CreatedTenant create(CreateTenantCommand c) {
        TenantStatus initial = c.initialState() == null ? TenantStatus.TRIAL : c.initialState();
        if (initial != TenantStatus.TRIAL && initial != TenantStatus.ACTIVE) {
            throw new BusinessRuleException("Manual tenant creation supports TRIAL or ACTIVE only");
        }
        String slug = SignupService.normalizeSlug(
                c.slug() != null && !c.slug().isBlank() ? c.slug() : c.schoolName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug already taken: " + slug);
        }

        LocalDateTime now = LocalDateTime.now();
        Tenant saved = tenantRepository.save(Tenant.builder()
                .schoolName(c.schoolName()).slug(slug).email(c.email()).phone(c.phone())
                .address(c.address()).city(c.city()).country(c.country()).director(c.director())
                .establishmentTypes(c.establishmentTypes() == null ? new java.util.LinkedHashSet<>() : c.establishmentTypes())
                .status(initial)
                .activatedAt(now)
                .trialEndsAt(initial == TenantStatus.TRIAL ? now.plusDays(30) : null)
                .createdAt(now).build());
        // Mot de passe : celui choisi par le SUPER_ADMIN, sinon un mot de passe temporaire lisible
        // (à changer à la 1re connexion) qu'on envoie par email à l'administrateur.
        boolean chosen = c.adminPassword() != null && !c.adminPassword().isBlank();
        String effectivePassword = chosen ? c.adminPassword() : TemporaryPasswords.generate();

        // iam provisions the (verified) ADMIN_ECOLE + roles; onboarding/communication via TenantActivated.
        eventPublisher.publishEvent(new TenantCreatedEvent(
                saved.getId(), saved.getSchoolName(), saved.getEmail(), saved.getDirector(),
                effectivePassword, !chosen, c.adminUsername(), saved.getPhone()));
        if (!chosen) {
            // Envoi des identifiants (email + mot de passe temporaire) à l'administrateur de l'école.
            eventPublisher.publishEvent(new TenantAdminCredentialsIssuedEvent(
                    saved.getId(), saved.getSchoolName(), saved.getEmail(), effectivePassword));
        }
        eventPublisher.publishEvent(new TenantActivatedEvent(
                saved.getId(), saved.getSchoolName(), saved.getEmail(), saved.getDirector()));
        // Le mot de passe temporaire n'est renvoyé (une seule fois) que s'il a été généré.
        return new CreatedTenant(saved, chosen ? null : effectivePassword);
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
        boolean brandingChanged = !Objects.equals(t.getLogo(), c.logo())
                || !Objects.equals(t.getBrandColor(), c.brandColor());
        t.setSchoolName(c.schoolName()); t.setLogo(c.logo()); t.setBrandColor(c.brandColor());
        t.setAddress(c.address()); t.setCity(c.city()); t.setCountry(c.country());
        t.setPhone(c.phone()); t.setEmail(c.email()); t.setWebsite(c.website()); t.setDirector(c.director());
        t.setAccreditationNumber(c.accreditationNumber()); t.setOpeningAuthorization(c.openingAuthorization());
        t.setMotto(c.motto()); t.setSlogan(c.slogan());
        t.setIre(c.ire()); t.setDpeLabel(c.dpeLabel()); t.setSousPrefectureLabel(c.sousPrefectureLabel());
        // Payout : ne pas écraser avec null (les éditions SUPER_ADMIN n'envoient pas ces champs).
        // Le formulaire de l'école envoie "" pour effacer, jamais null.
        if (c.payoutMerchantNumber() != null) t.setPayoutMerchantNumber(c.payoutMerchantNumber());
        if (c.payoutOperator() != null) t.setPayoutOperator(c.payoutOperator());
        if (c.payoutAccountName() != null) t.setPayoutAccountName(c.payoutAccountName());
        Tenant saved = tenantRepository.save(t);
        if (brandingChanged) {
            eventPublisher.publishEvent(new TenantBrandingUpdatedEvent(id));
        }
        return saved;
    }

    @Override
    public Tenant updateBranding(Long id, String logo, String brandColor) {
        Tenant t = findById(id);
        t.setLogo(logo);
        t.setBrandColor(brandColor);
        Tenant saved = tenantRepository.save(t);
        eventPublisher.publishEvent(new TenantBrandingUpdatedEvent(id));
        return saved;
    }

    @Override
    public void suspend(Long id) {
        lifecycleService.transition(id, TenantStatus.SUSPENDED, "manual suspension");
    }

    @Override
    public void activate(Long id) {
        lifecycleService.transition(id, TenantStatus.ACTIVE, "manual reactivation");
    }

    @Override
    public void changeStatus(Long id, TenantStatus target) {
        lifecycleService.transition(id, target, "status change");
    }

    @Override
    public void delete(Long id) {
        lifecycleService.transition(id, TenantStatus.ARCHIVED, "soft delete");
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findEstablishmentTypeNames(Long tenantId) {
        Tenant t = tenantRepository.findById(tenantId).orElse(null);
        if (t == null || t.getEstablishmentTypes() == null) return Set.of();
        return t.getEstablishmentTypes().stream().map(Enum::name).collect(Collectors.toSet());
    }
}
