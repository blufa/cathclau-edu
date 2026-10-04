package gn.lacaeducation.audit.adapters.out.persistence;

import gn.lacaeducation.audit.domain.AccountantAnomalyStatus;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accountant_anomaly_alerts")
@Getter @Setter @NoArgsConstructor
@Filter(name = "tenantFilter")
public class AccountantAnomalyAlertJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountantAnomalyType type;

    @Column(name = "metric_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal metricValue;

    @Column(name = "peer_average", nullable = false, precision = 15, scale = 2)
    private BigDecimal peerAverage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountantAnomalyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
