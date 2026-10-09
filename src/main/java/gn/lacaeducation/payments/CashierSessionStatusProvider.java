package gn.lacaeducation.payments;

/** Expose au reste de l'application si un comptable a une session de caisse ouverte — sans donner
 * accès au module interne {@code CashierSessionRepository} (autres modules : {@code expenses}, pour
 * bloquer la saisie de dépense tant que la caisse n'est pas ouverte, même principe que la barrière
 * déjà appliquée aux encaissements guichet dans {@code PaymentService}). */
public interface CashierSessionStatusProvider {
    boolean hasOpenSession(Long cashierId);
}
