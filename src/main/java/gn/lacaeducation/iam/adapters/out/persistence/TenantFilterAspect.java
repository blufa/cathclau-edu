package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.common.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Enables the global {@code tenantFilter} Hibernate filter on the active session before any Spring
 * Data JPA repository call, scoping every tenant-aware query to {@link TenantContext}.
 *
 * <p>When no tenant is bound (SUPER_ADMIN platform calls, scheduled jobs, unauthenticated payment
 * webhooks) the filter is left disabled, preserving the required cross-tenant access. Enabling an
 * already-enabled filter is a no-op that simply refreshes the parameter, so running before every
 * repository invocation is safe and keeps the scope correct even across nested calls.</p>
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } catch (RuntimeException ignored) {
            // No active Hibernate session bound to the thread yet — nothing to scope.
        }
    }
}
