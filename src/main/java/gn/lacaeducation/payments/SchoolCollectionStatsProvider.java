package gn.lacaeducation.payments;

import java.math.BigDecimal;
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
}
