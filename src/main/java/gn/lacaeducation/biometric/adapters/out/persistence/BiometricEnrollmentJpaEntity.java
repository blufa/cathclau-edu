package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.domain.BiometricSubjectType;
import gn.lacaeducation.biometric.domain.BiometricType;
import gn.lacaeducation.biometric.domain.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_enrollments")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BiometricEnrollmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "device_id", nullable = false) private Long deviceId;
    @Enumerated(EnumType.STRING) @Column(name = "subject_type", nullable = false, length = 16) private BiometricSubjectType subjectType;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Enumerated(EnumType.STRING) @Column(name = "biometric_type", nullable = false, length = 16) private BiometricType biometricType;
    @Column(name = "external_user_id", nullable = false, length = 64) private String externalUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private EnrollmentStatus status;
    @Column(name = "enrolled_at") private LocalDateTime enrolledAt;
}
