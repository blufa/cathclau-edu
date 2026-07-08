package gn.lacaeducation.transport.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "transport_routes")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TransportRouteJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 128) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "vehicle_registration", length = 32) private String vehicleRegistration;
    @Column(name = "driver_name", length = 128) private String driverName;
    @Column(name = "driver_phone", length = 32) private String driverPhone;
    private Integer capacity;
    @Column(name = "monthly_fee", precision = 15, scale = 2) private BigDecimal monthlyFee;
    @Column(nullable = false) private boolean active;
}
