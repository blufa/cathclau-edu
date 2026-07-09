package gn.lacaeducation.biometric.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un appareil biométrique enregistré (empreinte/visage) avec sa configuration. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricDevice {
    private Long id;
    private Long tenantId;
    private String name;
    private BiometricDeviceType deviceType;
    private String ipAddress;
    private Integer port;
    private BiometricProtocol protocol;
    private String location;
    private boolean active;
    private boolean realtimeSync;
    private Integer syncIntervalMinutes;
    private LocalDateTime lastSyncAt;
    private DeviceStatus status;
    private String accuracyLevel;
    private boolean maskAllowed;
    private boolean temperatureCheck;
    private BigDecimal temperatureThreshold;
    private String apiKey;
}
