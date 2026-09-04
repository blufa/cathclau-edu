package gn.lacaeducation.audit.application.listener;

import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase.RecordAuditCommand;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.application.event.PermissionsModifiedEvent;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.event.UserLoggedInEvent;
import gn.lacaeducation.iam.application.event.UserPermissionsModifiedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.platform.application.event.TenantArchivedEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Journalise dans {@code audit_logs} les événements applicatifs déjà publiés à cette fin par les
 * modules métier (leurs Javadoc mentionnent explicitement « Consumed by the audit trail » /
 * « Triggers ... audit », jusqu'ici sans consommateur réel — voir DPE_ARCHITECTURE_ANALYSIS.md §18).
 *
 * <p>Première tranche volontairement non-exhaustive : couvre l'authentification, les changements de
 * rôles/permissions, le cycle de vie des tenants et les paiements/remboursements — les actions les
 * plus sensibles listées au cahier des charges DPE §16. D'autres événements pourront être ajoutés
 * ici sans changer le schéma.</p>
 *
 * <p>Chaque écoute tourne après commit de la transaction d'origine, dans sa propre transaction : un
 * échec de journalisation ne doit jamais faire échouer l'action métier auditée.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogUseCase auditLogUseCase;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserLoggedIn(UserLoggedInEvent event) {
        save(event.tenantId(), event.userId(), "LOGIN", "User", event.userId(), null);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRoleAssigned(RoleAssignedEvent event) {
        // Le tenant n'est pas porté par l'événement : résolu depuis la requête d'origine (même
        // thread, TenantContext toujours peuplé à ce stade — voir doc de classe).
        save(TenantContext.get(), null, "ROLE_ASSIGNED", "User", event.userId(),
                "roleId=" + event.roleId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPermissionsModified(PermissionsModifiedEvent event) {
        save(TenantContext.get(), null, "PERMISSIONS_MODIFIED", "Role", event.roleId(), null);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserPermissionsModified(UserPermissionsModifiedEvent event) {
        save(TenantContext.get(), null, "PERMISSIONS_MODIFIED", "User", event.userId(), null);
    }

    // ── Cycle de vie des tenants : actions plateforme (tenant_id NULL, visibles uniquement dans le
    // journal SUPER_ADMIN — jamais dans le journal propre de l'école, cf. cahier des charges §16). ──

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTenantCreated(TenantCreatedEvent event) {
        // Ne jamais journaliser adminPassword/adminUsername (données sensibles) — uniquement
        // l'identité de l'école créée.
        save(null, null, "TENANT_CREATED", "Tenant", event.tenantId(),
                "schoolName=" + event.schoolName());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTenantSuspended(TenantSuspendedEvent event) {
        save(null, null, "TENANT_SUSPENDED", "Tenant", event.tenantId(), null);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTenantReactivated(TenantReactivatedEvent event) {
        save(null, null, "TENANT_REACTIVATED", "Tenant", event.tenantId(), null);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTenantArchived(TenantArchivedEvent event) {
        save(null, null, "TENANT_ARCHIVED", "Tenant", event.tenantId(), null);
    }

    // ── Paiements/remboursements : rattachés à l'école concernée (journal école, §16). ──

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        save(event.tenantId(), null, "PAYMENT_SUCCEEDED", "Payment", event.paymentId(),
                "invoiceId=" + event.invoiceId() + ";amount=" + event.amount());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        save(event.tenantId(), null, "PAYMENT_REFUNDED", "Payment", event.paymentId(),
                "invoiceId=" + event.invoiceId() + ";amount=" + event.amount());
    }

    private void save(Long tenantId, Long utilisateurId, String action, String ressource,
                       Long ressourceId, String details) {
        try {
            auditLogUseCase.record(new RecordAuditCommand(tenantId, utilisateurId, action, ressource,
                    ressourceId, details));
        } catch (RuntimeException e) {
            // La journalisation ne doit jamais remonter d'erreur à l'appelant métier.
            log.error("Échec d'écriture du journal d'audit (action={}, ressource={}, id={}) : {}",
                    action, ressource, ressourceId, e.getMessage());
        }
    }
}
