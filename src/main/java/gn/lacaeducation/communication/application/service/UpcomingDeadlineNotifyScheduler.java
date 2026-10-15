package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.communication.application.event.UpcomingPaymentDeadlinesDigestEvent;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;
import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Status;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.platform.TenantListPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Job quotidien : relance INTERNE au personnel (pas le SMS parent déjà existant via
 * {@code DeadlineCheckScheduler}/{@code EcheanceDepasseeEvent}, qui ne couvre que les échéances DÉJÀ
 * en retard) — le comptable n'avait aucun rappel poussé pour une échéance qui APPROCHE (élèves en
 * mensualité/tranches), seulement la possibilité d'aller consulter "Suivi des paiements" de sa propre
 * initiative (§retour comptable). Une seule notification groupée par jour par utilisateur concerné
 * (pas une par élève, confirmé par l'utilisateur) : "{N} élève(s) ont une échéance de paiement proche
 * cette semaine", lien direct vers l'écran déjà filtrable.
 *
 * <p>Même motif que {@code DeadlineCheckScheduler} (hors contexte de requête, boucle explicitement par
 * tenant en restaurant {@link TenantContext}) — exécuté après lui (7h, contre 6h pour les retards) pour
 * que les deux jobs ne se chevauchent jamais sur le même tenant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpcomingDeadlineNotifyScheduler {

    /** Personnel à prévenir : direction + comptabilité, même esprit que les alertes fondateur
     * existantes ({@code CommunicationEventListener}) — pas les enseignants/secrétaires. */
    private static final Set<String> RECIPIENT_ROLES = Set.of("SCHOOL_ADMIN", "DIRECTOR", "ACCOUNTANT");

    private final TenantListPort tenantListPort;
    private final PaymentReminderCandidatesService candidatesService;
    private final UserPublicInfoProvider userPublicInfoProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 7 * * *") // quotidien à 7h, après le job des retards (6h)
    public void notifyUpcomingDeadlines() {
        for (TenantListPort.TenantRef tenant : tenantListPort.listActiveTenantIds()) {
            TenantContext.set(tenant.id());
            try {
                notifyForTenant(tenant.id());
            } catch (Exception e) {
                log.error("Upcoming deadline digest failed for tenant {}: {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    /** Extrait (public) pour pouvoir être rejoué à la demande pour UN SEUL tenant (voir contrôleur,
     * endpoint de test "notify-upcoming-now") sans dupliquer la boucle tenant du job planifié. */
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
