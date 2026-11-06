package gn.lacaeducation.biometric.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Deux pointages du même sujet, sur deux appareils de localisations différentes, trop rapprochés
 * dans le temps pour être physiquement compatibles (§suivi-examen, SYNCHRONISATION_ET_CONFLITS.md
 * §4.2) — jamais résolu automatiquement, toujours routé vers une revue humaine avec les deux
 * pointages en tension.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricPunchConflict {
    private Long id;
    private Long tenantId;
    private BiometricSubjectType subjectType;
    private Long subjectId;
    private Long punchAId;
    private Long punchBId;
    private Long deviceAId;
    private Long deviceBId;
    private LocalDateTime detectedAt;
    private ConflictStatus status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
}
