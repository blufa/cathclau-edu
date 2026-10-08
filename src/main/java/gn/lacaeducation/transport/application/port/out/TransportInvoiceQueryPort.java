package gn.lacaeducation.transport.application.port.out;

import java.util.List;

/**
 * Lecture miroir (sans dépendance directe au module {@code billing}) des factures de type TRANSPORT
 * — permet de distinguer, parmi les élèves abonnés à une ligne, ceux qui ont déjà réellement réglé
 * au moins un mois de ceux qui n'ont encore rien payé (§retour comptable : "savoir qui a payé le
 * transport").
 */
public interface TransportInvoiceQueryPort {
    /** Élèves du tenant ayant au moins une facture de frais TRANSPORT avec un montant payé > 0. */
    List<Long> findStudentIdsWithPaidTransportInvoice(Long tenantId);
}
