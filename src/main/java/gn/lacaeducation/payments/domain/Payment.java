package gn.lacaeducation.payments.domain;
import gn.lacaeducation.common.BusinessRuleException;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    private Long id;
    private Long tenantId;
    private Long invoiceId;
    private Long studentId;
    /** Année scolaire de la facture réglée — évite qu'un paiement d'une année clôturée réapparaisse
     * dans l'année en cours. */
    private Long academicYearId;
    private String reference;
    private BigDecimal amount;
    private PaymentChannel channel;
    private PaymentType type;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    /** Paiement groupé : mois couverts (libellé), nombre de mois, reste à payer, et parts JSON
     *  [{invoiceId, amount}] à répartir au succès (cas en ligne). Null pour un paiement simple. */
    private String coveredMonths;
    private Integer monthsCount;
    private BigDecimal remainingAfter;
    private String groupLines;
    /** Utilisateur ayant physiquement encaissé (canal CASH uniquement) — imprimé sur le reçu. */
    private Long collectedByUserId;

    /**
     * Strict payment state machine (features/09-payments.md §9.1):
     * INITIATED → PENDING → (SUCCEEDED | FAILED). A SUCCEEDED payment can be REFUNDED.
     * Illegal transitions are rejected and mapped to HTTP 422.
     */
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED =
            new EnumMap<>(PaymentStatus.class);
    static {
        ALLOWED.put(PaymentStatus.INITIATED, Set.of(PaymentStatus.PENDING, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.PENDING, Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.SUCCEEDED, Set.of(PaymentStatus.REFUNDED));
        ALLOWED.put(PaymentStatus.FAILED, Set.of());
        ALLOWED.put(PaymentStatus.REFUNDED, Set.of());
    }

    public boolean canTransitionTo(PaymentStatus target) {
        return ALLOWED.getOrDefault(status, Set.of()).contains(target);
    }

    /** Applies a status transition, rejecting illegal ones (mapped to HTTP 422). */
    public void transitionTo(PaymentStatus target) {
        if (status == target) {
            return;
        }
        if (!canTransitionTo(target)) {
            throw new BusinessRuleException("Illegal payment transition: " + status + " → " + target);
        }
        this.status = target;
    }
}
