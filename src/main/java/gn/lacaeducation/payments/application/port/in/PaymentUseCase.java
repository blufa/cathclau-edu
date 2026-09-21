package gn.lacaeducation.payments.application.port.in;
import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface PaymentUseCase {
    record InitiatePaymentCommand(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type,
                                  /** Payer phone number — required by aggregator channels (e.g. Djomy). */
                                  String payerNumber,
                                  /** Utilisateur connecté ayant saisi l'encaissement — imprimé sur le reçu (« Opérateur »). */
                                  Long collectedByUserId,
                                  /** Année scolaire de la facture réglée (évite qu'un paiement d'une année clôturée réapparaisse dans l'année en cours). */
                                  Long academicYearId) {}
    Payment initiate(InitiatePaymentCommand command) throws JsonProcessingException;

    /**
     * Part d'un paiement groupé affectée à une facture-mois. {@code studentId} identifie l'élève
     * propriétaire de la facture (fratrie, §payments) ; {@code null} retombe sur le
     * {@link GroupPaymentCommand#studentId()} de la commande (paiement mono-élève classique).
     */
    /** {@code paidMonths} : mois "YYYY-MM" couverts par cette part sur une facture multi-mois (tranche
     * partiellement réglée), reportés sur {@code Invoice.paidMonths} — null/vide si non précisé. */
    record InvoiceShare(Long invoiceId, Long studentId, BigDecimal amount, List<String> paidMonths) {}
    /**
     * Paiement groupé de plusieurs mois (ou de l'année) en une fois : un seul paiement, un seul reçu
     * listant les mois réglés et le reste à payer. Au guichet (canal manuel) la confirmation est
     * immédiate ; chaque facture-mois est soldée via {@code GroupedInvoiceSettlementEvent}.
     */
    record GroupPaymentCommand(Long tenantId, Long studentId, PaymentChannel channel, String payerNumber,
                               String coverageLabel, Integer monthsCount, BigDecimal remainingAfter,
                               List<InvoiceShare> shares,
                               /** Utilisateur connecté ayant saisi l'encaissement — imprimé sur le reçu (« Opérateur »). */
                               Long collectedByUserId,
                               /** Année scolaire des factures réglées (toutes les parts sont supposées de la même année). */
                               Long academicYearId,
                               /** Clé d'idempotence fournie par le client (mode hors-ligne) : rejouer avec la même clé
                                *  après une coupure réseau renvoie le paiement déjà créé au lieu d'en créer un second. */
                               String idempotencyKey) {}
    Payment initiateGroup(GroupPaymentCommand command);

    /**
     * Confirme un paiement (groupé ou simple) en interrogeant Djomy à la demande, sans dépendre du
     * webhook : si la transaction est réussie, le paiement est confirmé (reçu + répartition). Idempotent.
     */
    Payment confirmViaDjomy(Long paymentId) throws JsonProcessingException;

    /** On-demand query of the live Djomy status for a payment (diagnostic; webhook stays authoritative). */
    DjomyStatusResponse checkDjomyStatus(Long paymentId) throws JsonProcessingException;

    record DjomyStatusResponse(Long paymentId, String externalTransactionId, String djomyStatus,
                               BigDecimal amount, String currency, boolean success) {}

    Payment confirmSuccess(Long paymentId);
    Payment markFailed(Long paymentId);
    Payment markRefunded(Long paymentId);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByReference(String reference);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
    List<Payment> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    /** Encaissements CASH d'un comptable sur une plage de dates — rapport « par comptable » et
     * rapprochement de caisse (§CashierSession). */
    record CashierCollectionsSummary(Long cashierId, BigDecimal total, List<Payment> payments) {}
    CashierCollectionsSummary cashierCollectionsSummary(Long cashierId, LocalDateTime from, LocalDateTime to);

    /** Ligne du rapport « encaissements par jour » : total (tous canaux) et nombre de paiements réussis
     * pour une journée donnée. */
    record DailyCollectionsLine(java.time.LocalDate date, BigDecimal total, int count) {}
    /** Rapport « encaissements par jour » (§angles morts comptables) : permet au fondateur de voir, jour
     * par jour, ce qui a été encaissé — quel que soit le canal ou que les frais soient réglés par tranche
     * ou en une fois — pour son contrôle mensuel. */
    record DailyCollectionsReport(BigDecimal grandTotal, List<DailyCollectionsLine> days) {}
    DailyCollectionsReport dailyCollectionsReport(LocalDateTime from, LocalDateTime to);
    void delete(Long id);
}
