package gn.lacaeducation.billing.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Une application (même partielle) d'un {@link StudentCredit} à une facture — l'historique traçable
 * exigé par §4 : "date, auteur, paiement d'origine, facture source, facture cible, montant et solde
 * restant". Le paiement/facture d'origine se retrouvent via {@link StudentCredit#getSourcePaymentId()}/
 * {@link StudentCredit#getSourceInvoiceId()} (une ligne {@code StudentCredit} par trop-perçu créé).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentCreditUsage {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long creditId;
    private Long targetInvoiceId;
    private BigDecimal amount;
    private Long appliedByUserId;
    private LocalDateTime appliedAt;
}
