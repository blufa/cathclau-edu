package gn.lacaeducation.biometric.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** L'enrôlement d'une personne (élève/enseignant) sur un appareil : mappe vers l'ID interne appareil. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricEnrollment {
    private Long id;
    private Long tenantId;
    private Long deviceId;
    private BiometricSubjectType subjectType;
    private Long subjectId;
    private BiometricType biometricType;
    private String externalUserId;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    /** Jeton opaque aléatoire encodé dans le QR de badge (remplace l'ancien triplet tenant|type|id
     * lisible et devinable) — null pour les enrôlements biométriques classiques (non-badge). */
    private String badgeToken;
}
