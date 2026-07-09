package gn.lacaeducation.discipline.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un incident disciplinaire concernant un élève : type, gravité, description et sanction éventuelle. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineIncident {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private LocalDate incidentDate;
    private DisciplineCategory category;
    private DisciplineSeverity severity;
    private String description;
    private String sanction;
    private Long reportedBy;
    private DisciplineStatus status;
    private boolean parentNotified;
    private LocalDateTime createdAt;
    /** Punition (mineure) ou sanction (administrative, susceptible de recours). */
    private MeasureKind measureKind;
    /** Mesure précise du règlement (retenue, blâme, exclusion temporaire…). */
    private DisciplineMeasure measure;
    /** Sursis : aucun / partiel / total. */
    private Suspension suspension;
    /** Durée d'exclusion temporaire (jours), le cas échéant. */
    private Integer exclusionDays;
    /** Instance ayant décidé (enseignant, chef, conseil de discipline, commission éducative). */
    private DecisionBody decisionBody;
}
