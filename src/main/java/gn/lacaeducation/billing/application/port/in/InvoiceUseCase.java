package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface InvoiceUseCase {
    record CreateInvoiceCommand(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount, Long academicYearId) {}
    record BulkGenerateCommand(Long tenantId, Long classId, Long feeId, Long academicYearId) {}
    /** Génère une facture par mois (échéance) du frais MENSUALITE pour un élève, idempotent par échéance. */
    record GenerateMonthlyCommand(Long tenantId, Long studentId, Long feeId, Long academicYearId) {}
    /** Échéance non facturée parce qu'entièrement couverte par une exonération/avantage personnel
     * (§billing) — aucune Invoice créée pour elle (voir InvoiceService#buildAndSaveInvoice), à afficher
     * néanmoins comme une ligne "Exonéré 100%" dans le même tableau que les vraies factures, pour que le
     * comptable ne la confonde jamais avec une échéance simplement pas encore générée. */
    record ExemptedPeriod(Long installmentId, String periodLabel, String discountReason) {}
    /** Résultat de {@link #generateMonthly} : les factures réellement créées, et les échéances
     * couvertes à 100% par une exonération, réévaluées à chaque appel (§billing — "recalcul
     * automatique" : une exonération accordée ou révoquée en cours d'année est reflétée dès le
     * prochain appel, sans action manuelle sur les échéances déjà traitées). */
    record GenerateMonthlyResult(List<Invoice> created, List<ExemptedPeriod> exemptedPeriods) {}
    /** Crée une facture pour un frais ponctuel (inscription, transport fixe, examen...). Retourne
     * {@code null} — et ne crée RIEN — si le frais est entièrement couvert par une exonération/avantage
     * personnel (même garde que {@link #generateMonthly} pour la mensualité échelonnée) : jamais de
     * facture à 0 GNF. */
    Invoice create(CreateInvoiceCommand command);
    GenerateMonthlyResult generateMonthly(GenerateMonthlyCommand command);
    /** Génère UNIQUEMENT les échéances demandées (pas forcément toutes celles configurées comme
     * {@link #generateMonthly}) — idempotent, celles déjà facturées sont silencieusement ignorées. Sert
     * au transport (§billing, retour comptable) : facturé au mois, à la demande, jamais d'avance pour
     * toute l'année — le comptable choisit un seul mois (le cas courant) ou plusieurs à la fois (un
     * parent qui avance 3-4 mois), jamais une saisie manuelle de montant. */
    GenerateMonthlyResult generateSelectedInstallments(GenerateMonthlyCommand command, List<Long> installmentIds);
    List<Invoice> generateBulk(BulkGenerateCommand command);
    /** Renvoie l'URL du PDF de la facture, en le générant/uploadant à la demande s'il manque encore. */
    String pdfUrl(Long invoiceId);
    /** Génère et renvoie les octets du PDF de la facture (pour affichage direct, sans Cloudinary). */
    byte[] pdfBytes(Long invoiceId);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByReference(String reference);
    List<Invoice> findByStudentId(Long studentId);
    List<Invoice> findByTenantId(Long tenantId);
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    List<Invoice> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    List<Invoice> findByFeeId(Long feeId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    Invoice applyPayment(Long invoiceId, BigDecimal amount);
    /**
     * Variante utilisée par un paiement groupé (guichet — plusieurs factures réglées en un seul
     * geste, ex. inscription + tranche 1 + tranche 2) : {@code notify=false} solde la facture sans
     * publier {@code InvoicePaymentAppliedEvent}, pour que le parent ne reçoive qu'UNE notification
     * consolidée pour tout le règlement (déjà envoyée via {@code PaymentSucceededEvent}) plutôt
     * qu'une par facture couverte.
     */
    Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify);
    /**
     * Variante qui précise en plus quels mois (parmi ceux de l'échéance liée, "YYYY-MM") ce règlement
     * couvre — pour une facture multi-mois payée en plusieurs fois, accumulés dans
     * {@link Invoice#getPaidMonths()} au fil des paiements successifs (jamais écrasés, jamais
     * dupliqués). {@code null}/vide : facture d'un seul mois, ou mois non précisés — sans effet sur
     * {@code paidMonths}.
     */
    Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify, List<String> paidMonths);
    /**
     * Variante qui précise en plus qui a physiquement encaissé (canal CASH uniquement) — affiché en
     * pied de facture PDF ("Encaissé par"). {@code null} : encaissement en ligne, ou collecteur non
     * précisé — le champ existant sur la facture n'est alors pas modifié.
     */
    Invoice applyPayment(Long invoiceId, BigDecimal amount, boolean notify, List<String> paidMonths, Long collectedByUserId);
    /**
     * Répercute un remboursement de paiement sur la facture : réduit `paidAmount` (jamais en dessous
     * de zéro) et recalcule solde/statut. Distinct de {@link #applyPayment} — ne publie PAS
     * `InvoicePaymentAppliedEvent` (écouté côté communication pour notifier le parent d'un paiement
     * reçu ; le déclencher sur un remboursement enverrait un SMS trompeur).
     */
    Invoice reversePayment(Long invoiceId, BigDecimal amount);
    /**
     * Annule une facture SANS la supprimer : passe le statut à CANCELLED et le solde restant à zéro,
     * mais conserve {@code totalAmount}/{@code paidAmount} pour l'historique. Distinct de
     * {@link #cancel}, qui supprime physiquement la facture. Utilisé par le transfert de classe
     * (mensualités de l'ancienne classe qui ne s'appliquent plus).
     */
    Invoice voidInvoice(Long invoiceId);
    /**
     * Reporte un crédit (ex. déjà payé sur une facture annulée par {@link #voidInvoice}) sur une autre
     * facture : augmente {@code paidAmount} et recalcule le solde. Ne publie aucun événement de
     * paiement (ce n'est pas un nouveau règlement — éviterait un SMS trompeur et un double comptage du
     * frais plateforme déjà comptabilisé lors du paiement d'origine). {@code reason} est tracé sur la
     * facture ({@code creditedAmount}/{@code creditReason}, §retour comptable) pour que le comptable
     * comprenne pourquoi une facture apparaît déjà réglée sans paiement correspondant.
     */
    Invoice applyCredit(Long invoiceId, BigDecimal amount, String reason);
    /** Applique une remise / bourse (montant + motif) et recalcule le reste à payer / le statut. */
    Invoice applyDiscount(Long invoiceId, BigDecimal amount, String reason);
    /** Inclut/exclut CETTE facture précise de la retenue automatique sur salaire (§payroll, modal
     * "Détail de la retenue" — retour utilisateur : "pas obligatoire de prendre tous les frais dans le
     * mois de sa fiche de paie", ex. un frais annexe facultatif Maillots/T-shirts/Tenue scolaire) —
     * n'affecte ni les autres factures de l'enfant, ni le réglage global du parent. Le parent règle
     * alors celle-ci normalement au guichet. */
    Invoice setExcludedFromStaffDeduction(Long invoiceId, boolean excluded);
    /** Comme {@link #setExcludedFromStaffDeduction}, mais pour TOUTES les factures "avantage personnel"
     * encore dues de CET élève, tous mois confondus — quand l'enfant lui-même est retiré/réintégré de
     * la retenue (§retour comptable : "si on retire l'enfant de la retenue, ces factures doivent être
     * actives, on doit pouvoir les encaisser"), contrairement à {@link #setExcludedFromStaffDeduction}
     * qui ne vise qu'une facture précise. */
    void setAllStaffBenefitInvoicesExcluded(Long studentId, boolean excluded);

    /** Rattrapage ponctuel (§retour comptable, "ces factures ne se grisent pas pour retenue sur
     * salaire") : retague {@code STAFF_BENEFIT} toute facture encore due (UNPAID/PARTIAL), de QUELQUE
     * type que ce soit (inscription, réinscription, annexe, mensualité...), d'un enfant de personnel
     * qui n'a actuellement AUCUNE remise ({@code discountSource == null}) — exactement la même règle
     * que {@link gn.lacaeducation.billing.application.service.InvoiceService} applique déjà à la
     * CRÉATION d'une facture, ici appliquée rétroactivement aux factures émises avant qu'elle existe
     * (ou avant que le lien parent↔personnel ait été établi), qu'aucune action ne retouche jamais
     * automatiquement après coup. Idempotent et sûr à relancer : ne touche jamais une facture qui a
     * déjà une remise (même 0% déjà taguée), ni une facture payée/annulée. */
    record StaffBenefitBackfillResult(int invoicesUpdated, java.util.List<Long> invoiceIds) {}

    StaffBenefitBackfillResult backfillStaffBenefitTagging(Long tenantId);

    /** Factures « fantômes » créées AVANT le correctif §billing/fondation (frais ponctuel entièrement
     * couvert par une exonération — discountAmount == totalAmount — qui créait quand même une facture
     * à 0 GNF au lieu de n'en créer aucune, contrairement à la mensualité échelonnée qui a toujours eu
     * cette garde). Lecture seule, pour aperçu avant suppression manuelle via {@link #delete} —
     * jamais supprimée automatiquement : le comptable valide la liste d'abord. */
    List<Invoice> findFullyExemptedDanglingInvoices(Long tenantId);

    /** {@code staffChild} : au moins un parent de cet élève est personnel de l'école (affiche ou non le
     * bouton retirer/réintégrer sur les écrans de facturation — fiche élève, modal d'encaissement).
     * {@code excluded} : cet élève est actuellement exclu de la retenue (réglage par enfant OU global
     * du parent, voir ParentAccessProvider#hasOptedOutOfStaffAutoDeduction). */
    record StaffChildDeductionStatus(boolean staffChild, boolean excluded) {}

    /** Statut courant pour les écrans de facturation (fiche élève, modal d'encaissement) — §retour
     * comptable : "on peut aussi faire l'action là-bas", pas seulement depuis le détail de la fiche de
     * paie. */
    StaffChildDeductionStatus staffChildDeductionStatus(Long studentId);

    /** Exclut (ou réintègre) cet élève de la retenue sur salaire — depuis un écran de facturation qui ne
     * connaît que l'élève (pas le salarié/employeeType/staffId, contrairement à
     * SalaryFeeDeductionUseCase#setChildStaffDeductionExcluded côté payroll) : résout lui-même TOUS les
     * parents personnel de cet élève. Rend aussi, dans le même mouvement, ses factures "avantage
     * personnel" encore dues immédiatement (dé)verrouillables au guichet (voir
     * {@link #setAllStaffBenefitInvoicesExcluded}) — §retour comptable : "ces factures doivent être
     * actives, on doit pouvoir les encaisser". Sans effet si cet élève n'est pas enfant de personnel
     * ({@code staffChild} faux). */
    void setStaffChildDeductionExcluded(Long studentId, boolean excluded);
    /**
     * Applique un paiement déjà perçu par un autre canal (ex. retenue sur salaire du personnel pour
     * les frais de ses enfants, §payroll ↔ billing) sur les factures MENSUALITE impayées/partielles
     * d'un élève, de la plus ancienne à la plus récente, jusqu'à épuisement du montant — pour que
     * facturation et perception réelle restent cohérentes. Sans notification parent (déjà informé par
     * un autre canal) et sans effet si le montant est nul/négatif ou si aucune facture n'est éligible.
     */
    void applyExternalPayment(Long tenantId, Long studentId, BigDecimal amount);
    /**
     * Solde restant dû sur les factures de mensualité de cet élève dont la remise appliquée est un
     * avantage personnel PARTIEL (§billing ↔ payroll, {@link gn.lacaeducation.billing.domain.DiscountSource#STAFF_BENEFIT})
     * — le « reste » que l'école ne prend pas en charge, destiné à être retenu automatiquement sur le
     * salaire du parent employé plutôt que facturé au guichet. 0 si aucune facture éligible.
     * <p>
     * Limité au mois de paie {@code periodStart} (dérivé en « Mois Année », ex. "Octobre 2026", comme
     * {@link Invoice#getPeriodLabel()} pour une mensualité éclatée mois par mois — voir
     * {@code InvoiceService#generateMonthly}) — une facture dont le libellé de période ne correspond pas
     * à un mois précis (tranche non éclatée, transport, cantine, examen…) reste comptée quel que soit
     * le mois demandé, elle n'est pas rattachée à un seul mois. Sans ce filtre, une mensualité entière
     * générée d'avance (9 factures dès l'inscription) ferait retenir le solde de TOUTE l'année sur un
     * seul salaire (§retour comptable, bug corrigé). {@code periodStart} peut être {@code null} (la
     * paie n'a pas toujours une date de période saisie) — {@code periodLabel} sert alors de repli.
     */
    BigDecimal outstandingStaffBenefitRemainder(Long studentId, java.time.LocalDate periodStart, String periodLabel);
    /**
     * Applique {@code amount} comme paiement précisément sur la ou les factures STAFF_BENEFIT
     * correspondant à ce mois de paie (même portée que {@link #outstandingStaffBenefitRemainder}) —
     * jamais une répartition générique « la plus ancienne d'abord » (voir {@link #applyExternalPayment}),
     * qui risquerait de solder le mauvais mois quand plusieurs factures partagent la même date
     * d'émission (cas d'une mensualité éclatée en une fois à l'inscription). Sans effet si le montant
     * est nul/négatif ou si aucune facture ne correspond.
     */
    void applyStaffBenefitRemainderPayment(Long studentId, java.time.LocalDate periodStart, String periodLabel, BigDecimal amount);
    /** Même périmètre que {@link #outstandingStaffBenefitRemainder}, mais ligne par ligne (une par
     * facture encore due) au lieu d'un seul total — pour le détail affiché au comptable. */
    List<gn.lacaeducation.billing.StudentFeePaymentApi.DeductedInvoiceLine> outstandingStaffBenefitInvoiceLines(
            Long studentId, java.time.LocalDate periodStart, String periodLabel);
    /** Frais facultatifs de la classe de l'élève n'ayant encore aucune facture — informatif uniquement
     * (voir {@link gn.lacaeducation.billing.StudentFeePaymentApi#optionalFeesNotYetInvoiced}). */
    List<gn.lacaeducation.billing.StudentFeePaymentApi.OptionalFeeNotice> optionalFeesNotYetInvoiced(Long studentId);
    /**
     * Applique la remise pour paiement annuel complet (§billing) sur toutes les factures restantes
     * (non soldées) d'un élève pour un frais donné, si une règle est active pour le type de ce frais.
     * Sans effet (renvoie les factures inchangées) s'il n'y a qu'une seule tranche restante ou si
     * aucune règle n'est applicable.
     */
    List<Invoice> applyEarlyPaymentDiscount(Long studentId, Long feeId);
    /**
     * Annule la remise pour paiement annuel complet précédemment appliquée par
     * {@link #applyEarlyPaymentDiscount} sur les factures restantes d'un élève pour un frais donné —
     * remet chaque facture à son montant initial. À appeler dès que la condition d'éligibilité cesse
     * (ex. le parent décoche un des mois de la scolarité), sans quoi la remise resterait acquise
     * indéfiniment même après un changement de sélection.
     */
    List<Invoice> removeEarlyPaymentDiscount(Long studentId, Long feeId);
    /** Avance d'une étape le recouvrement (relance 1 → relance 2 → exclusion). */
    Invoice advanceRecovery(Long invoiceId);
    /** Positionne directement l'étape de recouvrement. */
    Invoice setRecoveryStage(Long invoiceId, gn.lacaeducation.billing.domain.RecoveryStage stage);
    void cancel(Long id);
    void delete(Long id);
    boolean existsById(Long id);
}
