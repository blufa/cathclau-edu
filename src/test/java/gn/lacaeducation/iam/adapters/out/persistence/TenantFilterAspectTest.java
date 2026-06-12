package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.common.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantFilterAspectTest {

    @Mock private EntityManager entityManager;
    @Mock private Session session;
    @Mock private Filter filter;

    @InjectMocks private TenantFilterAspect aspect;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void enablesFilterWithBoundTenant() {
        TenantContext.set(42L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        aspect.enableTenantFilter();

        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", 42L);
    }

    @Test
    void doesNothingWhenNoTenantBound() {
        TenantContext.clear();

        aspect.enableTenantFilter();

        verifyNoInteractions(entityManager);
    }

    @Test
    void swallowsErrorWhenNoSessionAvailable() {
        TenantContext.set(42L);
        when(entityManager.unwrap(Session.class)).thenThrow(new IllegalStateException("no session"));

        aspect.enableTenantFilter(); // must not propagate

        verify(session, never()).enableFilter("tenantFilter");
    }
}
