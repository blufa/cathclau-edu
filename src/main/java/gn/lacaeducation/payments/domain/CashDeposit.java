package gn.lacaeducation.payments.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dépôt d'argent remis physiquement par un comptable au fondateur de l'école (§5 cahier des charges) —
 * distinct d'un {@link CashierSession}, qui rapproche la caisse d'UN comptable à la fin de son service :
 * ici, c'est l'argent déjà rapproché (ou non) qui change physiquement de mains, et la seule preuve que
 * le fondateur l'a réellement reçu est sa propre validation dans la plateforme (jamais automatique).
 * {@code method} réutilise {@link PaymentChannel} (CASH/ORANGE_MONEY/BANK_CARD — mêmes libellés que
 * l'encaissement au guichet, voir {@code PaymentChannel#isDeskManual()}) plutôt qu'un nouvel enum.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CashDeposit {
    private Long id;
    private Long tenantId;
    private Long accountantId;
    private BigDecimal amount;
    private String currency;
    private PaymentChannel method;
    /** Compte ou destination si disponible (ex. numéro Orange Money du fondateur, IBAN) — facultatif. */
    private String destinationAccount;
    private LocalDateTime depositedAt;
    private String transactionReference;
    private String comment;
    private CashDepositStatus status;
    private Long validatedBy;
    private LocalDateTime validatedAt;
    private String rejectionReason;
    /** Clé d'idempotence (mode hors-ligne / rechargement de page) : rejouer avec la même clé après une
     * coupure réseau renvoie le dépôt déjà créé au lieu d'en créer un second. */
    private String idempotencyKey;
    private LocalDateTime createdAt;

    public boolean isPending() { return status == CashDepositStatus.PENDING; }
}
