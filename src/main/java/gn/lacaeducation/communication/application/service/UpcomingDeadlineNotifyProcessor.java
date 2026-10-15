package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.event.UpcomingPaymentDeadlinesDigestEvent;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Status;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Traitement du rappel "échéance proche" pour UN tenant, dans SA PROPRE transaction — extrait de
 * {@link UpcomingDeadlineNotifyScheduler} pour la même raison que {@code OverdueInstallmentProcessor}
 * est extrait de {@code DeadlineCheckScheduler} : un appel {@code this.xxx()} depuis la boucle du
 * scheduler ne passe pas par le proxy Spring qui applique {@code @Transactional} (auto-invocation),
 * et {@code @TransactionalEventListener} (voir {@code InboxEventListener}) ne se déclenche QUE si une
 * transaction est active au moment de la publication — sans cette classe séparée, l'événement partait
 * bien mais la notification in-app n'était jamais créée, silencieusement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UpcomingDeadlineNotifyProcessor {

    /** Personnel à prévenir : direction + comptabilité, même esprit que les alertes fondateur
     * existantes ({@code CommunicationEventListener}) — pas les enseignants/secrétaires. */
    private static final Set<String> RECIPIENT_ROLES = Set.of("SCHOOL_ADMIN", "DIRECTOR", "ACCOUNTANT");

    private final PaymentReminderCandidatesService candidatesService;
    private final UserPublicInfoProvider userPublicInfoProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void notifyForTenant(Long tenantId) {
        List<Candidate> upcoming = candidatesService.listByTenant(tenantId).stream()
                .filter(c -> c.status() == Status.UPCOMING)
                .toList();
        if (upcoming.isEmpty()) return;

        List<Long> recipientUserIds = userPublicInfoProvider.findUserIdsByRoles(tenantId, RECIPIENT_ROLES);
        if (recipientUserIds.isEmpty()) return;

        log.info("Échéances proches : {} élève(s) pour le tenant {}, notification de {} destinataire(s)",
                upcoming.size(), tenantId, recipientUserIds.size());
        eventPublisher.publishEvent(new UpcomingPaymentDeadlinesDigestEvent(tenantId, recipientUserIds, upcoming.size()));
    }
}
