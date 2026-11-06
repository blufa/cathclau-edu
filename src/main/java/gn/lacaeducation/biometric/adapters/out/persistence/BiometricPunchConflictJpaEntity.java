package gn.lacaeducation.biometric.adapters.out.persistence;

import gn.lacaeducation.biometric.domain.BiometricSubjectType;
import gn.lacaeducation.biometric.domain.ConflictStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_punch_conflicts")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BiometricPunchConflictJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(name = "subject_type", nullable = false, length = 16) private BiometricSubjectType subjectType;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(name = "punch_a_id", nullable = false) private Long punchAId;
    @Column(name = "punch_b_id", nullable = false) private Long punchBId;
    @Column(name = "device_a_id", nullable = false) private Long deviceAId;
    @Column(name = "device_b_id", nullable = false) private Long deviceBId;
    @Column(name = "detected_at", nullable = false) private LocalDateTime detectedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConflictStatus status;
    @Column(name = "reviewed_by") private Long reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "review_note", columnDefinition = "text") private String reviewNote;
}
