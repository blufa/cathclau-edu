package gn.lacaeducation.communication.application.port.out;

import gn.lacaeducation.communication.application.service.PaymentReminderCandidatesService.Candidate;

import java.util.List;

/** Impression de la liste "Relances paiement" filtrée côté frontend (classe/statut/tranches) — même
 * en-tête que les autres documents officiels (reçu, bulletin...), voir {@code AdminDocumentLayout}.
 * Le frontend envoie les élèves exactement tels que filtrés à l'écran (WYSIWYG) plutôt que de laisser
 * le backend reconstruire des filtres séparément, qui risqueraient de diverger. */
public interface PaymentStatusListPdfGenerator {
    byte[] generate(Long tenantId, List<Candidate> candidates);
}
