package gn.lacaeducation.transport.application.port.out;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Lecture miroir (sans dépendance directe au module {@code billing}) des factures de type TRANSPORT
 * — permet de savoir, pour chaque élève abonné à une ligne, combien de mois il a déjà réellement
 * réglés (§retour comptable : "savoir qui a payé le transport", puis précisé en "le nombre de mois
 * payé").
 */
public interface TransportInvoiceQueryPort {
    /** Nombre de factures de frais TRANSPORT réglées (montant payé > 0), par élève — absent de la
     * map si l'élève n'a encore rien payé (jamais une entrée à 0). */
    Map<Long, Long> countPaidTransportMonthsByStudentId(Long tenantId);

    /** Élèves ayant réglé une facture TRANSPORT dont l'échéance liée tombe dans [from, to] — pour
     * filtrer "qui a payé le mois en cours" (§retour comptable) sur la liste des abonnés. */
    Set<Long> findStudentIdsPaidForMonth(Long tenantId, LocalDate from, LocalDate to);
}
