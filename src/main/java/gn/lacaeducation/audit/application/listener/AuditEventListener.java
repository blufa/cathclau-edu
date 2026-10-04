package gn.lacaeducation.audit.application.listener;

import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.in.AuditLogUseCase.RecordAuditCommand;
import gn.lacaeducation.billing.application.event.ExemptionGrantedEvent;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.application.event.PermissionsModifiedEvent;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.event.UserLoggedInEvent;
import gn.lacaeducation.iam.application.event.UserPermissionsModifiedEvent;
import gn.lacaeducation.lessons.application.event.LessonValidatedWithoutCheckinEvent;
import gn.lacaeducation.payments.application.event.CashCollectedWithoutOpenSessionEvent;
import gn.lacaeducation.payments.application.event.CashierSessionClosedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.platform.application.event.TenantArchivedEvent;
import gn.lacaeducation.platform.application.event.TenantCreatedEvent;
import gn.lacaeducation.platform.application.event.TenantReactivatedEvent;
import gn.lacaeducation.platform.application.event.TenantSuspendedEvent;
import gn.lacaeducation.students.application.event.EnrollmentAbandonedEvent;
import gn.lacaeducation.students.application.event.EnrollmentCancelledEvent;
import gn.lacaeducation.students.application.event.EnrollmentCreatedEvent;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.event.StudentArchivedEvent;
import gn.lacaeducation.students.application.event.StudentCreatedEvent;
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
 * <p>Étendue (§retour fondateurs — contrôle anti-fraude comptable) : couvre désormais aussi la
 * création/archivage d'élève, le cycle de vie de l'inscription, la génération de facture, l'octroi
 * d'exonération et la clôture de session de caisse (avec son écart, même nul) — pour qu'un élève
 * inscrit puis discrètement supprimé, ou un encaissement jamais rapproché, laisse toujours une
 * trace. D'autres événements pourront être ajoutés ici sans changer le schéma.</p>
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
        // collectedByUserId n'est renseigné que pour un encaissement CASH au guichet (null pour un
        // paiement en ligne, qui n'a pas d'« opérateur » humain) — trace qui a enregistré l'encaissement
        // dans le journal, condition nécessaire au rapprochement de caisse (§CashierSession).
        save(event.tenantId(), event.collectedByUserId(), "PAYMENT_SUCCEEDED", "Payment", event.paymentId(),
                "invoiceId=" + event.invoiceId() + ";amount=" + event.amount());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        save(event.tenantId(), null, "PAYMENT_REFUNDED", "Payment", event.paymentId(),
                "invoiceId=" + event.invoiceId() + ";amount=" + event.amount());
    }

    // ── Élèves/inscriptions (§retour fondateurs : traçabilité contre les inscriptions non déclarées —
    // un élève créé puis archivé sans jamais passer par une facture ne doit plus être invisible). ──

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStudentCreated(StudentCreatedEvent event) {
        save(event.tenantId(), null, "STUDENT_CREATED", "Student", event.studentId(),
                "studentNumber=" + event.studentNumber());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStudentArchived(StudentArchivedEvent event) {
        save(event.tenantId(), null, "STUDENT_ARCHIVED", "Student", event.studentId(), null);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        // Pas de tenantId porté par cet événement (voir sa Javadoc) : résolu depuis le contexte de la
        // requête d'origine, comme onRoleAssigned ci-dessus.
        save(TenantContext.get(), null, "ENROLLMENT_CREATED", "Enrollment", event.enrollmentId(),
                "studentId=" + event.studentId() + ";classId=" + event.classId() + ";type=" + event.type());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentValidated(EnrollmentValidatedEvent event) {
        save(event.tenantId(), null, "ENROLLMENT_VALIDATED", "Student", event.studentId(),
                "classId=" + event.classId() + ";academicYearId=" + event.academicYearId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCancelled(EnrollmentCancelledEvent event) {
        save(event.tenantId(), null, "ENROLLMENT_CANCELLED", "Student", event.studentId(),
                "classId=" + event.classId() + ";academicYearId=" + event.academicYearId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentAbandoned(EnrollmentAbandonedEvent event) {
        save(event.tenantId(), null, "ENROLLMENT_ABANDONED", "Student", event.studentId(),
                "classId=" + event.classId() + ";academicYearId=" + event.academicYearId());
    }

    // ── Factures/exonérations (§retour fondateurs : qui a généré quoi, qui a accordé quelle remise). ──

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoiceGenerated(InvoiceGeneratedEvent event) {
        // InvoiceIssuedEvent est publié en même temps par InvoiceService (voir sa Javadoc) : un seul
        // des deux est journalisé pour ne pas doubler la ligne d'audit par facture créée.
        save(event.tenantId(), null, "INVOICE_GENERATED", "Invoice", event.invoiceId(),
                "studentId=" + event.studentId() + ";reference=" + event.reference());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExemptionGranted(ExemptionGrantedEvent event) {
        save(event.tenantId(), event.validatedBy(), "EXEMPTION_GRANTED", "Exemption", event.exemptionId(),
                "studentId=" + event.studentId() + ";type=" + event.type() + ";percentage=" + event.percentage()
                        + ";amount=" + event.amount() + ";reason=" + event.reason());
    }

    // ── Rapprochement de caisse (§retour fondateurs, point central : un écart doit toujours laisser
    // une trace, même quand il est nul — l'absence de trace serait elle-même suspecte). ──

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCashierSessionClosed(CashierSessionClosedEvent event) {
        save(event.tenantId(), event.cashierId(), "CASHIER_SESSION_CLOSED", "CashierSession", event.sessionId(),
                "systemTotal=" + event.systemTotal() + ";physicalCount=" + event.physicalCount()
                        + ";discrepancy=" + event.discrepancy());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCashCollectedWithoutOpenSession(CashCollectedWithoutOpenSessionEvent event) {
        save(event.tenantId(), event.cashierId(), "CASH_COLLECTED_WITHOUT_OPEN_SESSION", "Payment", event.paymentId(),
                "amount=" + event.amount());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonValidatedWithoutCheckin(LessonValidatedWithoutCheckinEvent event) {
        save(event.tenantId(), null, "LESSON_VALIDATED_WITHOUT_CHECKIN", "LessonEntry", event.lessonId(),
                "teacherId=" + event.teacherId() + ";lessonDate=" + event.lessonDate());
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
