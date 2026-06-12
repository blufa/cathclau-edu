/**
 * Declares the global Hibernate filter used to enforce multi-tenant isolation.
 *
 * <p>Every tenant-scoped JPA entity (those carrying a {@code tenant_id} column) is annotated with
 * {@code @Filter(name = "tenantFilter")}. The filter is enabled per request by
 * {@link gn.lacaeducation.iam.adapters.out.persistence.TenantFilterAspect} using the current
 * {@link gn.lacaeducation.common.TenantContext}. When no tenant is bound (SUPER_ADMIN, background jobs,
 * unauthenticated payment webhooks) the filter is simply not enabled, giving cross-tenant access.</p>
 *
 * <p><b>Limitation:</b> Hibernate {@code @Filter} scopes HQL/criteria/derived queries (the list and
 * search surface) but does not apply to {@code EntityManager.find()} by primary key. Endpoints that
 * fetch by id and must stay tenant-scoped should verify the loaded entity's tenant against
 * {@link gn.lacaeducation.common.TenantContext} (done for users in the iam web layer).</p>
 *
 * <p>The {@code @FilterDef} is global: any module's entity can reference it by name without creating
 * a structural dependency on iam (the reference is a string, not a type import).</p>
 */
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = Long.class),
        defaultCondition = "tenant_id = :tenantId"
)
package gn.lacaeducation.iam.adapters.out.persistence;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
