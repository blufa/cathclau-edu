package gn.lacaeducation.dpe.domain;

import gn.lacaeducation.common.BusinessRuleException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** NOUVELLE → EN_COURS → RESOLUE. Une alerte résolue peut être rouverte automatiquement en
 * NOUVELLE si sa condition réapparaît lors d'une réévaluation (jamais via l'API, uniquement par
 * {@code DpeAlertService#evaluateForPrefecture}). */
public enum DpeAlertStatus {
    NOUVELLE, EN_COURS, RESOLUE;

    private static final Map<DpeAlertStatus, Set<DpeAlertStatus>> ALLOWED = new EnumMap<>(DpeAlertStatus.class);
    static {
        ALLOWED.put(NOUVELLE, Set.of(EN_COURS, RESOLUE));
        ALLOWED.put(EN_COURS, Set.of(RESOLUE));
        // Réouverture manuelle (« finalement pas résolu ») : seul chemin retour autorisé via l'API.
        // Le retour à NOUVELLE n'est jamais déclenché manuellement — uniquement par la réévaluation
        // automatique (DpeAlertService), qui écrit le statut directement sans passer par cette garde.
        ALLOWED.put(RESOLUE, Set.of(EN_COURS));
    }

    public void requireTransitionAllowed(DpeAlertStatus target) {
        if (this == target) {
            return;
        }
        if (!ALLOWED.getOrDefault(this, Set.of()).contains(target)) {
            throw new BusinessRuleException("Transition de statut d'alerte non autorisée : " + this + " → " + target);
        }
    }
}
