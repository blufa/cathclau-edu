package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Type d'évaluation configurable par établissement (§3) : l'école crée ses propres types
 * (Interrogation, Devoir, Composition, Contrôle continu, Oral, TP…) avec, pour chacun, un
 * coefficient d'évaluation par défaut, un poids, une couleur d'affichage et un état actif.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentTypeConfig {
    private Long id;
    private Long tenantId;
    /** Code court unique par tenant (ex. DEVOIR, COMPO). */
    private String code;
    private String label;
    /** Coefficient d'évaluation par défaut appliqué aux évaluations de ce type. */
    private BigDecimal defaultCoefficient;
    /** Poids (%) éventuel dans une moyenne par catégorie. */
    private BigDecimal weight;
    /** Couleur d'affichage (hex, ex. #2E7D32). */
    private String color;
    private boolean active;
}
