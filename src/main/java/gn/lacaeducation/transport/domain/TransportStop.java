package gn.lacaeducation.transport.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Un arrêt d'une ligne de transport, avec les heures de ramassage/dépose. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportStop {
    private Long id;
    private Long tenantId;
    private Long routeId;
    private String name;
    private LocalTime pickupTime;
    private LocalTime dropTime;
    private Integer sequence;
}
