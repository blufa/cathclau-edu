package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.event.TenantSignupRequestedEvent;
import gn.lacaeducation.platform.application.port.in.SignupUseCase;
import gn.lacaeducation.platform.application.port.in.SubscriptionUseCase;
import gn.lacaeducation.platform.application.port.out.EmailVerificationTokenRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.EmailVerificationToken;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Self-service onboarding of a school (features/01-platform.md §1): public signup, email verification
 * (PENDING_VERIFICATION → TRIAL via {@link TenantLifecycleService}), resend and public branding.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SignupService implements SignupUseCase {

    private final TenantRepository tenantRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final TenantLifecycleService lifecycleService;
    private final PlanRepository planRepository;
    private final SubscriptionUseCase subscriptionUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.platform.trial-days:30}")
    private int trialDays;

    @Value("${app.platform.verification-token-hours:48}")
    private int verificationTokenHours;

    @Override
    public SignupResult signup(SignupCommand c) {
        String slug = normalizeSlug(c.desiredSlug() != null && !c.desiredSlug().isBlank()
                ? c.desiredSlug() : c.schoolName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug already taken: " + slug);
        }
        if (c.adminEmail() != null && tenantRepository.existsByEmail(c.adminEmail())) {
            throw new ConflictException("A school already exists for this email: " + c.adminEmail());
        }

        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .schoolName(c.schoolName()).slug(slug).email(c.adminEmail()).director(c.director())
                .city(c.city()).country(c.country())
                .establishmentTypes(c.establishmentType() == null
                        ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(java.util.List.of(c.establishmentType())))
                .status(TenantStatus.PENDING_VERIFICATION)
                .requestedPlanCode(c.desiredPlan())
                .trialEndsAt(now.plusDays(trialDays))
                .createdAt(now).build());

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepository.save(EmailVerificationToken.builder()
                .tenantId(tenant.getId()).email(c.adminEmail()).token(token)
                .expiresAt(now.plusHours(verificationTokenHours)).createdAt(now).build());

        // IAM creates the unverified ADMIN_ECOLE (with password); communication emails the link.
        eventPublisher.publishEvent(new TenantSignupRequestedEvent(
                tenant.getId(), tenant.getSchoolName(), slug, c.adminEmail(), c.director(),
                c.password(), token, c.adminFirstName(), c.adminLastName(), c.adminPhone()));

        return new SignupResult(tenant.getId(), slug, tenant.getStatus(),
                "Inscription enregistrée. Vérifiez votre email pour activer l'école.");
    }

    @Override
    public void verifyEmail(String token) {
        EmailVerificationToken evt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (evt.isConsumed()) {
            return; // idempotent: already verified
        }
        if (evt.isExpired()) {
            throw new BusinessRuleException("Verification token has expired; request a new one");
        }

        // PENDING_VERIFICATION → TRIAL; lifecycle sets activatedAt and publishes TenantActivated.
        lifecycleService.transition(evt.getTenantId(), TenantStatus.TRIAL, "email verified");

        evt.setConsumedAt(LocalDateTime.now());
        tokenRepository.save(evt);

        // If the school chose a paid plan at signup, auto-create its subscription (PENDING) + invoice
        // so it can pay (Djomy) and be activated. A trial/default plan keeps the school on free trial.
        autoSubscribeForPaidPlan(evt.getTenantId());
    }

    private void autoSubscribeForPaidPlan(Long tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            String code = tenant.getRequestedPlanCode();
            if (code == null || code.isBlank()) return;
            if (subscriptionUseCase.findActive(tenantId).isPresent()) return; // already subscribed
            PlanCode planCode;
            try {
                planCode = PlanCode.valueOf(code.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown requested plan code '{}' for tenant {}", code, tenantId);
                return;
            }
            planRepository.findByCode(planCode).ifPresent(plan -> {
                if (Boolean.TRUE.equals(plan.getDefaultTrial())) return; // free trial plan → no paid subscription
                try {
                    subscriptionUseCase.initiate(new SubscriptionUseCase.InitiateSubscriptionCommand(
                            tenantId, plan.getId(), BillingPeriod.MONTHLY, true));
                    log.info("Auto-created PENDING subscription for tenant {} on plan {}", tenantId, plan.getCode());
                } catch (RuntimeException e) {
                    log.warn("Auto-subscription failed for tenant {}: {}", tenantId, e.getMessage());
                }
            });
        });
    }

    @Override
    public void resendVerification(String email) {
        Tenant tenant = tenantRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No school found for email: " + email));
        if (tenant.getStatus() != TenantStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("School is already verified");
        }

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepository.save(EmailVerificationToken.builder()
                .tenantId(tenant.getId()).email(email).token(token)
                .expiresAt(now.plusHours(verificationTokenHours)).createdAt(now).build());

        // Renvoi du lien : l'admin existe déjà (provisioning idempotent), pas besoin des infos admin.
        eventPublisher.publishEvent(new TenantSignupRequestedEvent(
                tenant.getId(), tenant.getSchoolName(), tenant.getSlug(), email,
                tenant.getDirector(), null, token, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandingView getBranding(String slug) {
        Tenant t = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + slug));
        return new BrandingView(t.getId(), t.getSlug(), t.getSchoolName(), t.getLogo(), t.getBrandColor());
    }

    /** ASCII, lower-case, hyphen-separated slug derived from the requested value or school name. */
    static String normalizeSlug(String raw) {
        String n = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return n.isBlank() ? "ecole" : n;
    }
}
