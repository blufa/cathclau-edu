package gn.lacaeducation.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable) : un comptable dont un
 * indicateur (écarts de caisse, volume d'exonérations) dépasse nettement la moyenne de ses pairs
 * du même établissement. Une ligne par (tenant, comptable, type) — réévaluée et mise à jour en
 * place chaque semaine par {@code AccountantAnomalyAlertService}, jamais dupliquée.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountantAnomalyAlert {
    private Long id;
    private Long tenantId;
    private Long cashierId;
    private AccountantAnomalyType type;
    private BigDecimal metricValue;
    private BigDecimal peerAverage;
    private AccountantAnomalyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
