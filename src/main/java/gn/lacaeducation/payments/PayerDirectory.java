package gn.lacaeducation.payments;

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
    /** Libellé de l'année scolaire courante du tenant (ex. « 2025-2026 »), imprimé sur le reçu officiel. */
    Optional<String> currentAcademicYearLabel(Long tenantId);

    /** Type de frais réglé (« Inscription », « Mensualité »…) et, pour une mensualité, le mois couvert. */
    record InvoiceFeeInfo(String feeTypeLabel, String periodLabel) {}

    /** Nature du frais couvert par une facture (pour l'observation « Inscription »/« Mensualité » du reçu). */
    Optional<InvoiceFeeInfo> invoiceFeeInfo(Long invoiceId);
}
