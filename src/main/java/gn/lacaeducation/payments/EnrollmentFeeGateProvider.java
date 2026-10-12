package gn.lacaeducation.payments;

import java.util.List;

/**
 * API publique du module payments : factures ENROLLMENT/RE_ENROLLMENT encore dues d'un élève (§retour
 * comptable, demande utilisateur — "la facture Inscription/Réinscription doit être réglée avant ou en
 * même temps que les autres frais"). Définie ici (pas dans billing, qui possède Invoice/SchoolFee) car
 * {@code payments} est en AMONT de {@code billing} dans le graphe de dépendances (billing dépend déjà
 * de payments pour réagir aux événements de paiement) — un import payments → billing fermerait un
 * cycle interdit par Spring Modulith. L'implémentation vit donc côté billing (voir
 * {@code EnrollmentFeeGateAdapter}), qui dépend déjà de payments sans ajouter de nouvelle arête.
 */
public interface EnrollmentFeeGateProvider {

    /** Ids des factures ENROLLMENT/RE_ENROLLMENT encore dues (solde restant &gt; 0) de cet élève —
     * vide si aucune n'existe ou si toutes sont déjà réglées. */
    List<Long> outstandingEnrollmentInvoiceIds(Long studentId);
}
