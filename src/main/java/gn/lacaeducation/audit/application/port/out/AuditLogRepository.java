package gn.lacaeducation.audit.application.port.out;

import gn.lacaeducation.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {
    AuditLog save(AuditLog log);
    /** Filtré au tenant courant (résolu via le filtre Hibernate tenantFilter) — vue école. */
    Page<AuditLog> findForCurrentTenant(Pageable pageable);
    /** Toutes les actions plateforme (tenant_id IS NULL) — vue SUPER_ADMIN. */
    Page<AuditLog> findPlatformActions(Pageable pageable);
    /** Toutes les lignes, tous tenants confondus — vue SUPER_ADMIN. */
    Page<AuditLog> findAll(Pageable pageable);
}
