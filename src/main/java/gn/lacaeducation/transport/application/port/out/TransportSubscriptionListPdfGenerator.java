package gn.lacaeducation.transport.application.port.out;

import gn.lacaeducation.transport.application.service.TransportService.SubscriptionPrintRow;

import java.util.List;

/** Impression de la liste des abonnés transport filtrée côté frontend (ligne, mois payé) — même
 * en-tête que les autres documents officiels, voir {@code AdminDocumentLayout}. */
public interface TransportSubscriptionListPdfGenerator {
    byte[] generate(Long tenantId, List<SubscriptionPrintRow> rows);
}
