package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.domain.BiometricDeviceType;
import gn.lacaeducation.biometric.domain.BiometricProtocol;
import gn.lacaeducation.biometric.domain.DeviceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_devices")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BiometricDeviceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 128) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "device_type", nullable = false, length = 16) private BiometricDeviceType deviceType;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    private Integer port;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private BiometricProtocol protocol;
    @Column(length = 128) private String location;
    @Column(nullable = false) private boolean active;
    @Column(name = "realtime_sync", nullable = false) private boolean realtimeSync;
    @Column(name = "sync_interval_minutes", nullable = false) private Integer syncIntervalMinutes;
    @Column(name = "last_sync_at") private LocalDateTime lastSyncAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private DeviceStatus status;
    @Column(name = "accuracy_level", length = 16) private String accuracyLevel;
    @Column(name = "mask_allowed", nullable = false) private boolean maskAllowed;
    @Column(name = "temperature_check", nullable = false) private boolean temperatureCheck;
    @Column(name = "temperature_threshold", precision = 4, scale = 1) private BigDecimal temperatureThreshold;
    @Column(name = "api_key", length = 64) private String apiKey;
}
