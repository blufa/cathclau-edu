package gn.lacaeducation.billing.domain;

/**
 * Origine de la remise en pourcentage effectivement appliquée à une facture (la plus avantageuse
 * parmi réduction familiale / avantage personnel / exonération, voir
 * {@code InvoiceService#computeApplicableDiscount}) — jamais renseignée pour une remise EXCEPTIONAL
 * (montant fixe, qui s'ajoute en plus mais ne "gagne" jamais la comparaison en %). Sert notamment à
 * répercuter automatiquement le solde non couvert par un avantage personnel PARTIEL sur la paie du
 * parent employé (§billing ↔ payroll).
 */
public enum DiscountSource {
    FAMILY,
    STAFF_BENEFIT,
    EXEMPTION
}
