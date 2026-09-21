package gn.lacaeducation.communication.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * File d'attente de renvoi des SMS échoués (§retour direction — connexion serveur↔fournisseur SMS
 * instable) : un SMS qui échoue à l'envoi (Nimba injoignable, timeout réseau…) est mis en file ici au
 * lieu d'être simplement perdu, et renvoyé automatiquement en tâche de fond jusqu'à réussite ou
 * épuisement des tentatives (voir {@code SmsRetryDispatchScheduler}) — même principe que
 * {@code WebhookEvent} (§webhooks), appliqué ici au canal SMS.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRetryEntry {
    private Long id;
    private Long tenantId;
    /** Lien vers le destinataire d'origine (MessageRecipient) — permet de refléter le succès/échec
     * définitif sur son statut de livraison une fois la file traitée. Null si l'appelant n'a pas de
     * destinataire suivi individuellement (ex. futur usage hors module communication). */
    private Long recipientId;
    private String destination;
    private String subject;
    private String content;
    private SmsRetryStatus status;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
}
