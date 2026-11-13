package gn.lacaeducation.payments;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * API publique du module payments pour le reporting financier (§10.2) : historique des encaissements
 * réussis d'un tenant, agrégés par mois — base du prévisionnel de trésorerie.
 */
public interface SchoolCollectionStatsProvider {

    /** Total encaissé (paiements réussis) sur un mois civil. */
    record MonthlyCollection(int year, int month, BigDecimal amount) {}

    /** Les {@code months} derniers mois civils (du plus ancien au plus récent), y compris à zéro. */
    List<MonthlyCollection> monthlyCollections(Long tenantId, int months);

    /** Total encaissé (paiements réussis) d'un mois civil précis, passé ou présent — contrairement à
     * {@link #monthlyCollections}, qui ne couvre qu'une fenêtre glissante se terminant au mois en
     * cours, utilisable pour rapprocher un encaissement du même mois qu'une autre statistique
     * (ex. masse salariale réellement payée, §retour comptable "Encaissé net"). */
    BigDecimal collectedTotal(Long tenantId, int year, int month);

    /** Un paiement réussi encaissé ce jour-là — détail ligne par ligne (§retour comptable : "voici les
     * factures encaissées, voici l'argent collecté" pour un jour précis), contrairement à
     * {@link #monthlyCollections} qui n'agrège qu'un total par mois. {@code studentId} résolu en nom
     * par l'appelant (voir StudentDataProvider), jamais d'identifiant brut affiché côté écran. */
    record DailyCollectionLine(Long paymentId, Long studentId, BigDecimal amount, String channel, String reference) {}

    List<DailyCollectionLine> dailyCollectionLines(Long tenantId, LocalDate date);
}
