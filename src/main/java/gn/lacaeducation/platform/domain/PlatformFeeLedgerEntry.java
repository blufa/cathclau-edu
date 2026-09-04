package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registre d'append : une ligne par part de frais plateforme effectivement encaissée par une école
 * (au prorata de chaque paiement de mensualité appliqué), créée depuis
 * {@code PlatformFeeCollectedEvent} (billing). Jamais modifiée ni supprimée — le solde dû par une
 * école est la somme de ses lignes moins la somme de ses règlements réussis
 * ({@link PlatformFeeSettlement}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeLedgerEntry {
    private Long id;
    private Long tenantId;
    private Long invoiceId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
