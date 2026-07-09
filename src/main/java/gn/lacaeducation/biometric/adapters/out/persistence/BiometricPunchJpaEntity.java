package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.domain.BiometricSubjectType;
import gn.lacaeducation.biometric.domain.PunchDirection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_punches")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BiometricPunchJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "device_id", nullable = false) private Long deviceId;
    @Column(name = "external_user_id", nullable = false, length = 64) private String externalUserId;
    @Enumerated(EnumType.STRING) @Column(name = "subject_type", length = 16) private BiometricSubjectType subjectType;
    @Column(name = "subject_id") private Long subjectId;
    @Column(name = "punch_time", nullable = false) private LocalDateTime punchTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private PunchDirection direction;
    @Column(precision = 4, scale = 1) private BigDecimal temperature;
    @Column(name = "raw_payload", columnDefinition = "text") private String rawPayload;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
