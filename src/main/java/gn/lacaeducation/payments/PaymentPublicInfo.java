package gn.lacaeducation.payments;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/** {@code coveredMonths} : libellé bref de ce que ce paiement couvre (ex. "Octobre, Novembre" ou
 * "Inscription + Tranche 1"), déjà calculé à l'initiation d'un paiement groupé — null pour un
 * paiement simple (une seule facture, déjà nommée ailleurs). */
public record PaymentPublicInfo(Long id, String reference, BigDecimal amount, String channel,
                                LocalDateTime paidAt, String coveredMonths) {}
