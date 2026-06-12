package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.port.in.PlatformDashboardUseCase;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import gn.lacaeducation.platform.domain.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformDashboardService implements PlatformDashboardUseCase {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;

    @Override
    public PlatformDashboard getDashboard() {
        var tenants = tenantRepository.findAll();
        long totalTenants = tenants.size();
        long active = tenants.stream().filter(t -> t.getStatus() == TenantStatus.ACTIVE).count();
        long suspended = tenants.stream().filter(t -> t.getStatus() == TenantStatus.SUSPENDED).count();
        long archived = tenants.stream().filter(t -> t.getStatus() == TenantStatus.ARCHIVED).count();

        List<Subscription> subs = subscriptionRepository.findAll();
        Map<String, Long> byStatus = subs.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPlan = subs.stream()
                .collect(Collectors.groupingBy(s -> String.valueOf(s.getPlanId()), Collectors.counting()));

        // MRR: monthly-equivalent revenue of ACTIVE subscriptions (annual amounts divided by 12).
        BigDecimal mrr = subs.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && s.getAmount() != null)
                .map(this::monthlyEquivalent)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal arr = mrr.multiply(TWELVE);

        LocalDate today = LocalDate.now();
        long expiringSoon = subs.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE && s.getEndDate() != null)
                .filter(s -> !s.getEndDate().isBefore(today) && !s.getEndDate().isAfter(today.plusDays(7)))
                .count();

        long cancelled = byStatus.getOrDefault(SubscriptionStatus.CANCELLED.name(), 0L);
        double churnRate = totalSubs(subs) == 0 ? 0.0
                : BigDecimal.valueOf(cancelled).divide(BigDecimal.valueOf(totalSubs(subs)), 4, RoundingMode.HALF_UP).doubleValue();

        var monthStart = today.withDayOfMonth(1).atStartOfDay();
        var successfulThisMonth = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESSFUL && p.getPaidAt() != null
                        && !p.getPaidAt().isBefore(monthStart))
                .toList();
        long paymentsThisMonth = successfulThisMonth.size();
        BigDecimal revenueThisMonth = successfulThisMonth.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new PlatformDashboard(totalTenants, active, suspended, archived,
                subs.size(), byStatus, byPlan, mrr, arr, expiringSoon, churnRate,
                paymentsThisMonth, revenueThisMonth);
    }

    private long totalSubs(List<Subscription> subs) {
        return subs.size();
    }

    private BigDecimal monthlyEquivalent(Subscription s) {
        long days = ChronoUnit.DAYS.between(s.getStartDate(), s.getEndDate());
        boolean annual = days > 32;
        return annual ? s.getAmount().divide(TWELVE, 2, RoundingMode.HALF_UP) : s.getAmount();
    }
}
