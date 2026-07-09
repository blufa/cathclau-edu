package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.domain.SubscriptionStatus;
import gn.lacaeducation.transport.domain.TransportDirection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_subscriptions")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TransportSubscriptionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "route_id", nullable = false) private Long routeId;
    @Column(name = "stop_id") private Long stopId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private TransportDirection direction;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "monthly_fee", precision = 15, scale = 2) private BigDecimal monthlyFee;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SubscriptionStatus status;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
