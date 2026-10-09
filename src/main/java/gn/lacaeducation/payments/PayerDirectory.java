package gn.lacaeducation.payments;

import java.util.List;
import java.util.Optional;

/**
 * SPI publique du module {@code payments} : résout le nom de l'élève payeur pour l'imprimer sur le
 * reçu (snapshot au moment du règlement). Implémentée par un module qui connaît les élèves
 * ({@code billing}), ce qui évite une dépendance directe payments → students (cycle).
 */
public interface PayerDirectory {
    Optional<String> studentName(Long studentId);
    /** Matricule de l'élève, imprimé sur le reçu officiel. */
    Optional<String> studentNumber(Long studentId);
    /** Libellé de la classe courante de l'élève (ex. « 7ème A »), imprimé sur le reçu officiel. */
    Optional<String> className(Long studentId);
    /**
     * Nom de l'agent ayant encaissé (« Opérateur » sur le reçu). Passe par ce port (plutôt qu'une
     * dépendance directe payments → iam) pour éviter un cycle : platform dépend déjà de payments
     * (Djomy), et iam de platform (événements) — une dépendance directe payments → iam boucle.
     */
    Optional<String> operatorName(Long userId);
    /** Téléphone de l'agent ayant encaissé, imprimé sous son nom dans le bloc signature du reçu
     * (§retour comptable) — distinct du téléphone de l'école affiché en en-tête. */
    Optional<String> operatorPhone(Long userId);
    /** Libellé de l'année scolaire courante du tenant (ex. « 2025-2026 »), imprimé sur le reçu officiel. */
    Optional<String> currentAcademicYearLabel(Long tenantId);

    /**
     * Type de frais réglé (« Inscription », « Mensualité »…), le mois couvert pour une mensualité, et
     * la remise éventuellement appliquée à cette facture (ex. remise paiement anticipé) — pour que le
     * reçu affiche toujours la réduction, même quand elle est calculée automatiquement.
     */
    record InvoiceFeeInfo(String feeTypeLabel, String periodLabel,
                          java.math.BigDecimal discountAmount, String discountReason,
                          /** Part de la facture due au frais d'usage plateforme (§billing) — null/zéro
                           * si l'élève n'a pas accepté l'accès plateforme. Affichée sur le reçu pour
                           * que la ligne ne disparaisse jamais silencieusement. */
                          java.math.BigDecimal platformFeeAmount) {}

    /** Nature du frais couvert par une facture (pour l'observation « Inscription »/« Mensualité » du reçu). */
    Optional<InvoiceFeeInfo> invoiceFeeInfo(Long invoiceId);

    /** Une facture encore due (impayée ou partielle) d'un élève — libellé lisible (ex. « Mensualité —
     * Novembre 2026 ») et solde restant, pour énumérer le « reste à payer » dans le SMS de confirmation
     * d'encaissement (§retour comptable, point 1) sans se limiter à la seule facture réglée. */
    record UnpaidInvoiceInfo(String label, java.math.BigDecimal remaining) {}

    /** Toutes les factures encore dues de l'élève sur l'année scolaire courante du tenant (une
     * facture sans année renseignée reste incluse plutôt que silencieusement perdue), triées par date
     * d'émission croissante. Vide si tout est réglé. Volontairement limité à l'année courante — une
     * dette d'une année révolue ne doit pas gonfler le "reste à payer" affiché sur un reçu du moment
     * (§retour comptable). */
    List<UnpaidInvoiceInfo> unpaidInvoices(Long studentId);

    /** Une facture de l'élève — quel que soit son statut (sauf annulée) — pour le tableau complet du
     * reçu de paiement (§retour comptable) : le parent voit d'un coup d'œil toutes les factures de son
     * enfant, réglées ou non, pas seulement celle(s) couvertes par le paiement en cours. {@code amount}
     * reste le montant BRUT de la facture (avant remise, pour rester cohérent avec la note "Remise
     * appliquée : -X GNF" imprimée séparément) ; {@code paidAmount} est le montant NET réellement
     * encaissé à ce jour sur cette facture (ce qui a été payé, jamais plus) — c'est lui qu'il faut
     * sommer pour un total cumulé "Total payé" correct, jamais {@code amount} (qui, pour une facture
     * remisée, est plus élevé que ce qui a réellement été reçu).
     * {@code creditedAmount}/{@code creditReason} : montant porté au paidAmount SANS paiement réel
     * correspondant (ex. crédit reporté d'une ancienne classe lors d'un transfert) — sans cette trace,
     * le reçu afficherait une facture déjà réglée sans qu'aucun encaissement ne l'explique.
     * {@code remainingBalance} : solde encore dû sur CETTE facture précisément (net, après remise) —
     * affiché en colonne "Reste" sur le reçu, pour qu'un règlement partiel sur une tranche (§retour
     * comptable : un parent qui règle 300 000 GNF sur une tranche de 340 000) soit immédiatement
     * clair pour le parent, sans qu'il ait à comparer lui-même "Montant" et "Payé". */
    record InvoiceRowInfo(Long id, String reference, java.math.BigDecimal amount, String label, boolean paid,
                          java.math.BigDecimal paidAmount,
                          java.math.BigDecimal creditedAmount, String creditReason,
                          java.time.LocalDateTime creditedAt, java.math.BigDecimal remainingBalance) {}

    /** Toutes les factures non annulées de l'élève sur l'année scolaire courante du tenant (même
     * portée que {@link #unpaidInvoices}) — inscription/réinscription d'abord, puis la scolarité
     * (tranches, chronologique), puis les frais annexes (§retour comptable, reçu de paiement). */
    List<InvoiceRowInfo> studentInvoices(Long studentId);

    /** Surplus de crédit laissé par un transfert de classe (nouvelle classe moins chère que
     * l'ancienne), pas encore remboursé au parent — voir {@code ClassTransferRefund}. */
    record PendingRefundInfo(java.math.BigDecimal amount, String reason, java.time.LocalDateTime createdAt) {}

    /** Surplus(s) encore dus à CET élève (pas encore marqués remboursés) — affichés sur le reçu
     * jusqu'à ce que le comptable confirme le remboursement physique au parent (§retour comptable :
     * un surplus resté seulement dans les logs/SMS ne se retrouvait plus jamais nulle part après
     * coup). Vide si aucun surplus en attente. */
    List<PendingRefundInfo> pendingRefunds(Long studentId);
}
