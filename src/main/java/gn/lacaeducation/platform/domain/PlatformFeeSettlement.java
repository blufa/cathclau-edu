package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Règlement, par une école, du solde qu'elle doit à la plateforme (bouton « Verser » du dashboard
 * finance → redirection Djomy). Même mécanique que {@link SubscriptionPayment} : création en
 * INITIATED, redirection via Djomy (PENDING), confirmation manuelle au retour ou vérification de
 * statut (SUCCESSFUL/FAILED) — jamais de webhook générique ici, comme pour l'abonnement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeSettlement {
    private Long id;
    private Long tenantId;
    private String reference;
    private BigDecimal amount;
    private PaymentChannel channel;
    private PaymentStatus status;
    private String externalTransactionId;
    /** Djomy aggregator redirect URL returned to the frontend. */
    private String redirectUrl;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
