package gn.lacaeducation.audit.adapters.in.web.dto;

import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import gn.lacaeducation.audit.domain.AccountantAnomalyStatus;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountantAnomalyAlertResponse(
        Long id,
        Long cashierId,
        AccountantAnomalyType type,
        BigDecimal metricValue,
        BigDecimal peerAverage,
        AccountantAnomalyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
    public static AccountantAnomalyAlertResponse from(AccountantAnomalyAlert a) {
        return new AccountantAnomalyAlertResponse(a.getId(), a.getCashierId(), a.getType(), a.getMetricValue(),
                a.getPeerAverage(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt(), a.getResolvedAt());
    }
}
