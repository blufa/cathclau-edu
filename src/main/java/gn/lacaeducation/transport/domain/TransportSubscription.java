package gn.lacaeducation.transport.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** L'abonnement d'un élève à une ligne (et un arrêt) de transport. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportSubscription {
    private Long id;
    private Long tenantId;
    private Long routeId;
    private Long stopId;
    private Long studentId;
    private TransportDirection direction;
    private LocalDate startDate;
    private BigDecimal monthlyFee;
    private SubscriptionStatus status;
    private LocalDateTime createdAt;
}
