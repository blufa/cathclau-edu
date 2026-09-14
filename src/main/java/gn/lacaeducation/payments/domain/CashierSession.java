package gn.lacaeducation.payments.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Session de caisse d'un comptable au guichet (§angles morts comptables — rapprochement de caisse) :
 * de l'ouverture à la clôture, compare le total système (paiements CASH encaissés par ce comptable
 * pendant la session) au comptant physique compté, pour détecter un écart entre l'argent réellement
 * en caisse et ce qui a été enregistré. Pas de fonds de caisse initial : chaque session démarre à zéro.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CashierSession {
    private Long id;
    private Long tenantId;
    private Long cashierId;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    /** Somme des paiements CASH réussis de ce comptable entre l'ouverture et la clôture — calculé à la
     * clôture, null tant que la session est ouverte. */
    private BigDecimal systemTotal;
    /** Comptant physique compté par le comptable à la clôture. */
    private BigDecimal physicalCount;
    /** {@code physicalCount - systemTotal} — 0 si la caisse est juste, positif en excédent, négatif en
     * manquant. */
    private BigDecimal discrepancy;
    private String notes;
    private CashierSessionStatus status;
    private LocalDateTime createdAt;
}
