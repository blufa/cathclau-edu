package gn.lacaeducation.transport.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "transport_stops")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TransportStopJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "route_id", nullable = false) private Long routeId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "pickup_time") private LocalTime pickupTime;
    @Column(name = "drop_time") private LocalTime dropTime;
    @Column(nullable = false) private Integer sequence;
}
