package gn.lacaeducation.payments;

import java.util.Set;

/** Expose au reste de l'application si un comptable a une session de caisse ouverte — sans donner
 * accès au module interne {@code CashierSessionRepository} (autres modules : {@code expenses}, pour
 * bloquer la saisie de dépense tant que la caisse n'est pas ouverte, même principe que la barrière
 * déjà appliquée aux encaissements guichet dans {@code PaymentService}). */
public interface CashierSessionStatusProvider {
    boolean hasOpenSession(Long cashierId);

    /** Rôles dispensés de la barrière caisse (encaissement guichet comme saisie de dépense) : la
     * direction encaisse/dépense parfois en direct sans passer par le rituel ouverture/clôture réservé
     * au personnel de guichet (demande utilisateur, "il ne faut pas lui bloquer"). Partagé par
     * {@code PaymentService} et {@code MonthlyExpenseSheetService}. */
    Set<String> EXEMPT_CASHIER_GATE_ROLES = Set.of("SCHOOL_ADMIN", "SUPER_ADMIN");
}
