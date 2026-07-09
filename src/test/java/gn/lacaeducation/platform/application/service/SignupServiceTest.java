package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.platform.application.event.TenantSignupRequestedEvent;
import gn.lacaeducation.platform.application.port.in.SignupUseCase.BrandingView;
import gn.lacaeducation.platform.application.port.in.SignupUseCase.SignupCommand;
import gn.lacaeducation.platform.application.port.in.SignupUseCase.SignupResult;
import gn.lacaeducation.platform.application.port.out.EmailVerificationTokenRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.EmailVerificationToken;
import gn.lacaeducation.platform.domain.EstablishmentType;
import gn.lacaeducation.platform.domain.Tenant;
import gn.lacaeducation.platform.domain.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock EmailVerificationTokenRepository tokenRepository;
    @Mock TenantLifecycleService lifecycleService;
    @Mock ApplicationEventPublisher eventPublisher;

    SignupService service;

    @BeforeEach
    void setUp() {
        service =new SignupService(tenantRepository, tokenRepository, lifecycleService, null, null, eventPublisher);
        ReflectionTestUtils.setField(service, "trialDays", 30);
        ReflectionTestUtils.setField(service, "verificationTokenHours", 48);
    }

    @Test
    void signup_createsPendingTenant_tokenAndEvent() {
        when(tenantRepository.existsBySlug("ecole-claire")).thenReturn(false);
        when(tenantRepository.existsByEmail("dir@ecole.gn")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(inv -> { Tenant t = inv.getArgument(0); t.setId(7L); return t; });
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));
        // Ce test couvre le flux avec vérification email (PENDING + token).
        ReflectionTestUtils.setField(service, "requireEmailVerification", true);

        SignupResult result = service.signup(new SignupCommand(
                "École Claire", "École Claire", "dir@ecole.gn", "admin", "password123",
                "Conakry", "Guinée", java.util.Set.of(EstablishmentType.COLLEGE), "Mamadou Diallo", null, null, null, null));

        assertThat(result.tenantId()).isEqualTo(7L);
        assertThat(result.slug()).isEqualTo("ecole-claire");
        assertThat(result.status()).isEqualTo(TenantStatus.PENDING_VERIFICATION);

        ArgumentCaptor<Tenant> tCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tCaptor.capture());
        assertThat(tCaptor.getValue().getStatus()).isEqualTo(TenantStatus.PENDING_VERIFICATION);
        assertThat(tCaptor.getValue().getTrialEndsAt()).isNotNull();

        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(eventPublisher).publishEvent(any(TenantSignupRequestedEvent.class));
    }

    @Test
    void signup_duplicateSlug_conflict() {
        when(tenantRepository.existsBySlug("ecole-claire")).thenReturn(true);

        assertThatThrownBy(() -> service.signup(new SignupCommand(
                "École Claire", "ecole-claire", "dir@ecole.gn", "admin", "password123",
                null, null, java.util.Set.of(EstablishmentType.LYCEE), "Dir", null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void verifyEmail_consumesToken_andTransitionsToTrial() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(1L).tenantId(7L).email("dir@ecole.gn").token("abc")
                .expiresAt(LocalDateTime.now().plusHours(10)).createdAt(LocalDateTime.now()).build();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        service.verifyEmail("abc");

        verify(lifecycleService).transition(7L, TenantStatus.TRIAL, "email verified");
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getConsumedAt()).isNotNull();
    }

    @Test
    void verifyEmail_alreadyConsumed_isIdempotentNoOp() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tenantId(7L).token("abc").consumedAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(10)).build();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        service.verifyEmail("abc");

        verify(lifecycleService, org.mockito.Mockito.never())
                .transition(any(Long.class), any(), any());
    }

    @Test
    void verifyEmail_expired_isRejected() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tenantId(7L).token("abc").expiresAt(LocalDateTime.now().minusHours(1)).build();
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("abc"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getBranding_returnsPublicView() {
        Tenant t = Tenant.builder().id(7L).slug("ecole-claire").schoolName("École Claire")
                .logo("logo.png").brandColor("#00f").status(TenantStatus.TRIAL).build();
        when(tenantRepository.findBySlug("ecole-claire")).thenReturn(Optional.of(t));

        BrandingView view = service.getBranding("ecole-claire");

        assertThat(view.schoolName()).isEqualTo("École Claire");
        assertThat(view.logo()).isEqualTo("logo.png");
        assertThat(view.brandColor()).isEqualTo("#00f");
    }

    @Test
    void normalizeSlug_stripsAccentsAndSymbols() {
        assertThat(SignupService.normalizeSlug("Lycée Sainte-Marie")).isEqualTo("lycee-sainte-marie");
    }
}
