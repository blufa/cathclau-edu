package gn.lacaeducation.communication.application.event;

import gn.lacaeducation.communication.domain.CommunicationChannel;

import java.util.List;

/**
 * Publié par {@code CommunicationService#send} une fois la communication et ses destinataires créés
 * en base (statut PENDING) — l'envoi effectif (appel réseau au fournisseur SMS/email/WhatsApp, parfois
 * lent) est déclenché en tâche de fond par {@code CommunicationDispatchListener}, après la validation
 * (commit) de la transaction courante, pour ne jamais bloquer la requête HTTP qui a déclenché la
 * notification (paiement, inscription, absence…). Usage strictement interne au module {@code
 * communication} — ne porte que des identifiants, le listener relit l'état frais en base.
 */
public record CommunicationRecipientsPendingEvent(
        Long communicationId, CommunicationChannel channel, String subject, String content, List<Long> recipientIds) {}
