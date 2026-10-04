package gn.lacaeducation.audit.application.port.in;

import gn.lacaeducation.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogUseCase {

    AuditLog record(RecordAuditCommand command);

    /** Vue école (ADMIN_ECOLE/DIRECTOR) : ses propres lignes, résolues via le tenant courant. */
    Page<AuditLog> findForCurrentTenant(Pageable pageable);

    /** Vue école filtrée (action/utilisateur/période), chaque paramètre optionnel. */
    Page<AuditLog> searchForCurrentTenant(String action, Long utilisateurId, LocalDateTime from,
                                           LocalDateTime to, Pageable pageable);

    /** Vue SUPER_ADMIN : actions plateforme uniquement (tenant_id IS NULL). */
    Page<AuditLog> findPlatformActions(Pageable pageable);

    /** Vue SUPER_ADMIN : toutes les lignes, tous tenants confondus. */
    Page<AuditLog> findAll(Pageable pageable);

    record RecordAuditCommand(Long tenantId, Long utilisateurId, String action, String ressource,
                               Long ressourceId, String details) {}
}
