package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trop-perçu conservé comme crédit disponible de l'élève (§4 cahier des charges) : un paiement
 * supérieur au solde dû d'une facture n'est plus perdu/mal imputé — l'excédent est posé ici et reste
 * applicable à une facture future (voir {@code StudentCreditUseCase#applyToInvoice}), jusqu'à
 * épuisement ({@link #remainingAmount} atteint zéro). {@link #amount} (montant d'origine, immuable)
 * et {@link #remainingAmount} (solde encore disponible, décrémenté à chaque usage) divergent dès la
 * première application partielle — voir {@code StudentCreditUsage} pour le détail de chaque usage.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentCredit {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private BigDecimal amount;
    private BigDecimal remainingAmount;
    /** Paiement à l'origine du trop-perçu, si connu (encaissement direct ; null pour une source
     * différente, ex. report manuel). */
    private Long sourcePaymentId;
    /** Facture sur laquelle le paiement excédentaire a été initialement enregistré. */
    private Long sourceInvoiceId;
    private String reason;
    private LocalDateTime createdAt;

    public boolean isExhausted() {
        return remainingAmount == null || remainingAmount.signum() <= 0;
    }
}
