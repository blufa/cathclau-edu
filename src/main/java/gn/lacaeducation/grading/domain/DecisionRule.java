package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Règle de décision paramétrable (§17) : « SI &lt;condition&gt; ALORS &lt;décision&gt; ». Évaluée par
 * priorité par le moteur de règles. Activable/désactivable et versionnée par plage de dates (§8),
 * afin de ne pas recalculer les anciens bulletins lorsqu'une règle change.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DecisionRule {
    private Long id;
    private Long tenantId;
    private String name;
    private DecisionConditionType conditionType;
    /** Seuil de la condition (moyenne). */
    private BigDecimal threshold;
    /** Matière ciblée (pour SUBJECT_BELOW). */
    private Long subjectId;
    /** Nombre minimum de matières (pour SUBJECTS_BELOW_COUNT). */
    private Integer minCount;
    /** Décision produite si la condition est satisfaite (ex. ADMIS, REDOUBLE, RATTRAPAGE). */
    private String decision;
    /** Ordre d'évaluation : la plus petite priorité est évaluée en premier. */
    private int priority;
    private boolean active;
    /** Versionnement (§8) : plage de validité (null = pas de borne). */
    private LocalDate validFrom;
    private LocalDate validTo;
}
