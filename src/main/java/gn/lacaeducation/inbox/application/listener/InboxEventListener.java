package gn.lacaeducation.inbox.application.listener;

import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.communication.application.event.UpcomingPaymentDeadlinesDigestEvent;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.inbox.application.port.in.NotificationUseCase;
import gn.lacaeducation.inbox.application.port.in.NotificationUseCase.CreateNotificationCommand;
import gn.lacaeducation.inbox.domain.NotificationType;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.students.ParentAccessProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Génère les notifications in-app (§13) des parents à partir des événements métier : bulletin publié,
 * absence/retard, paiement enregistré. Le destinataire est résolu vers les comptes parents de l'élève.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventListener {

    private final NotificationUseCase notificationUseCase;
    private final ParentAccessProvider parentAccessProvider;

    @TransactionalEventListener
    @Async
    public void onReportCardPublished(ReportCardPublishedEvent event) {
        notifyParents(event.tenantId(), event.studentId(), NotificationType.REPORT_CARD,
                "Nouveau bulletin disponible",
                "Le bulletin de votre enfant a été publié.",
                "/parent/children/" + event.studentId() + "/documents");
    }

    @TransactionalEventListener
    @Async
    public void onStudentAbsent(StudentAbsentEvent event) {
        // StudentAbsentEvent couvre désormais aussi PRESENT (premier scan du jour, pour le SMS) —
        // la cloche in-app reste volontairement limitée aux absences/retards, comme avant.
        if ("PRESENT".equals(event.status())) return;
        notifyParents(event.tenantId(), event.studentId(), NotificationType.ABSENCE,
                "Absence / retard signalé",
                "Une absence ou un retard a été enregistré pour votre enfant le " + event.date() + ".",
                null);
    }

    @TransactionalEventListener
    @Async
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        notifyParents(event.tenantId(), event.studentId(), NotificationType.PAYMENT,
                "Paiement enregistré",
                "Un paiement de " + event.amount() + " a été enregistré.",
                null);
    }

    /** Rappel interne (direction/comptabilité, pas les parents) — échéances de paiement qui approchent,
     * une seule notification groupée par utilisateur et par jour (§retour comptable, voir javadoc de
     * {@code UpcomingDeadlineNotifyScheduler}). Les destinataires sont déjà résolus par l'émetteur. */
    @TransactionalEventListener
    @Async
    public void onUpcomingPaymentDeadlinesDigest(UpcomingPaymentDeadlinesDigestEvent event) {
        String content = event.studentCount() + " élève" + (event.studentCount() > 1 ? "s ont" : " a")
                + " une échéance de paiement proche cette semaine.";
        for (Long userId : event.userIds()) {
            try {
                notificationUseCase.create(new CreateNotificationCommand(event.tenantId(), userId, NotificationType.PAYMENT,
                        "Échéances proches", content, "/enrollments/payment-status"));
            } catch (Exception e) {
                log.error("Échec de création du rappel d'échéances proches pour user {} (tenant {}): {}",
                        userId, event.tenantId(), e.getMessage(), e);
            }
        }
    }

    private void notifyParents(Long tenantId, Long studentId, NotificationType type,
                               String title, String content, String link) {
        List<Long> parentUserIds = parentAccessProvider.findParentUserIdsByStudent(studentId);
        for (Long userId : parentUserIds) {
            try {
                notificationUseCase.create(new CreateNotificationCommand(tenantId, userId, type, title, content, link));
            } catch (Exception e) {
                log.error("Échec de création de la notification in-app pour user {} (élève {}): {}",
                        userId, studentId, e.getMessage(), e);
            }
        }
    }
}
