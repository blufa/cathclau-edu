package gn.lacaeducation.payments;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

    /** Vrai si l'utilisateur actuellement authentifié (requête HTTP en cours) porte un rôle exempté.
     * Lit directement les {@code GrantedAuthority} de Spring Security (déjà posées par
     * {@code SecurityConfig#extractAuthorities}, qui étend SUPER_ADMIN à tous les rôles personnel —
     * donc ROLE_SCHOOL_ADMIN y est aussi présent pour lui) plutôt que d'interroger le module iam :
     * {@code payments} est en amont de {@code iam} dans le graphe de dépendances (iam → platform →
     * payments), un appel {@code payments → iam} créerait un cycle interdit par Spring Modulith. */
    static boolean currentUserExempt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .anyMatch(EXEMPT_CASHIER_GATE_ROLES::contains);
    }
}
