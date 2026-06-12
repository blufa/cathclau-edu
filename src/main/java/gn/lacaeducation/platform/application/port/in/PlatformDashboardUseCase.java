package gn.lacaeducation.platform.application.port.in;

import java.math.BigDecimal;
import java.util.Map;

public interface PlatformDashboardUseCase {
    PlatformDashboard getDashboard();

    /** Aggregated editor-level KPIs (features/01-platform.md §12.1). */
    record PlatformDashboard(
            long totalTenants,
            long activeTenants,
            long suspendedTenants,
            long archivedTenants,
            long totalSubscriptions,
            Map<String, Long> subscriptionsByStatus,
            Map<String, Long> subscriptionsByPlan,
            BigDecimal mrr,
            BigDecimal arr,
            long expiringSoon,
            double churnRate,
            long paymentsThisMonth,
            BigDecimal revenueThisMonth
    ) {}
}
