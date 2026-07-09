package gn.lacaeducation.transport.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Une ligne de transport scolaire (avec son véhicule et son chauffeur). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private String vehicleRegistration;
    private String driverName;
    private String driverPhone;
    private Integer capacity;
    private BigDecimal monthlyFee;
    private boolean active;
}
