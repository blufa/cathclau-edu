package gn.lacaeducation.communication.application.event;

import java.util.List;

/** Un ou plusieurs élèves (mensualité/tranches) ont une échéance de paiement proche pour un tenant —
 * digest quotidien groupé (une notification in-app par utilisateur concerné, pas une par élève, voir
 * {@code UpcomingDeadlineNotifyScheduler}). Les destinataires sont déjà résolus (userId) au moment de
 * la publication : ce module sait déjà "qui est comptable/direction de ce tenant"
 * ({@code UserPublicInfoProvider}), contrairement aux autres événements consommés par {@code inbox}
 * (ex. paiement d'un élève) où c'est {@code inbox} qui résout les destinataires (parents de l'élève). */
public record UpcomingPaymentDeadlinesDigestEvent(Long tenantId, List<Long> userIds, int studentCount) {}
