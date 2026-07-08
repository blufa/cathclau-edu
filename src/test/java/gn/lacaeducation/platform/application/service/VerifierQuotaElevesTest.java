package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanCode;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifierQuotaElevesTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PlanRepository planRepository;
    @Mock SubscriptionInvoiceRepository invoiceRepository;
    @Mock SubscriptionPaymentRepository paymentRepository;
    @Mock SubscriptionReceiptRepository receiptRepository;
    @Mock PlanChangeRepository planChangeRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AbonnementService abonnementService;

    @Test
    void isQuotaAvailable_starter_respectsLimit500() {
        Subscription subscription = Subscription.builder()
                .id(1L).tenantId(10L).planId(1L)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(1))
                .amount(BigDecimal.valueOf(25000)).createdAt(LocalDateTime.now()).build();

        Plan starterPlan = Plan.builder()
                .id(1L).name("Starter").code(PlanCode.STARTER).studentLimit(500).build();

        when(subscriptionRepository.findActiveByTenantId(10L)).thenReturn(Optional.of(subscription));
        when(planRepository.findById(1L)).thenReturn(Optional.of(starterPlan));

        assertThat(abonnementService.isQuotaAvailable(10L, 499)).isTrue();
        assertThat(abonnementService.isQuotaAvailable(10L, 500)).isFalse();
    }

    @Test
    void isQuotaAvailable_premium_alwaysTrue() {
        Subscription subscription = Subscription.builder()
                .id(2L).tenantId(10L).planId(3L)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(1))
                .amount(BigDecimal.valueOf(150000)).createdAt(LocalDateTime.now()).build();

        Plan premiumPlan = Plan.builder()
                .id(3L).name("Premium").code(PlanCode.PREMIUM).studentLimit(null).build();

        when(subscriptionRepository.findActiveByTenantId(10L)).thenReturn(Optional.of(subscription));
        when(planRepository.findById(3L)).thenReturn(Optional.of(premiumPlan));

        assertThat(abonnementService.isQuotaAvailable(10L, 9999)).isTrue();
        assertThat(abonnementService.isQuotaAvailable(10L, 100000)).isTrue();
    }

    @Test
    void isQuotaAvailable_noSubscriptionNoTrialPlan_allowsTrial() {
        when(subscriptionRepository.findActiveByTenantId(99L)).thenReturn(Optional.empty());

        // Sans abonnement payant et sans plan d'essai par défaut, on n'empêche pas une école en
        // phase d'essai de créer des élèves (choix produit).
        assertThat(abonnementService.isQuotaAvailable(99L, 0)).isTrue();
    }
}
