package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Surplus de crédit laissé par un transfert de classe (§retour comptable) : un parent qui avait déjà
 * réglé plus que ce que coûtent les mensualités de la nouvelle classe se retrouve avec un montant
 * qu'aucune facture ne peut plus absorber — à rembourser physiquement, pas à laisser filer
 * silencieusement. Reste visible (sur le reçu, §OpenPdfReceiptGenerator) jusqu'à être marqué
 * remboursé ({@code refundedAt} renseigné) par le comptable/l'admin école.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClassTransferRefund {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long oldClassId;
    private Long newClassId;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime createdAt;
    /** Null tant que le remboursement physique n'a pas été confirmé. */
    private LocalDateTime refundedAt;

    public boolean isPending() {
        return refundedAt == null;
    }
}
