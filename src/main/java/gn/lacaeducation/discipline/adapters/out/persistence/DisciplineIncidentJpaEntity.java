package gn.lacaeducation.discipline.adapters.out.persistence;

import gn.lacaeducation.discipline.domain.DisciplineCategory;
import gn.lacaeducation.discipline.domain.DisciplineSeverity;
import gn.lacaeducation.discipline.domain.DisciplineStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "discipline_incidents")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class DisciplineIncidentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "class_id") private Long classId;
    @Column(name = "incident_date", nullable = false) private LocalDate incidentDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private DisciplineCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private DisciplineSeverity severity;
    @Column(columnDefinition = "text", nullable = false) private String description;
    @Column(columnDefinition = "text") private String sanction;
    @Column(name = "reported_by") private Long reportedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private DisciplineStatus status;
    @Column(name = "parent_notified", nullable = false) private boolean parentNotified;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING) @Column(name = "measure_kind", length = 16) private gn.lacaeducation.discipline.domain.MeasureKind measureKind;
    @Enumerated(EnumType.STRING) @Column(name = "measure_type", length = 32) private gn.lacaeducation.discipline.domain.DisciplineMeasure measure;
    @Enumerated(EnumType.STRING) @Column(length = 16) private gn.lacaeducation.discipline.domain.Suspension suspension;
    @Column(name = "exclusion_days") private Integer exclusionDays;
    @Enumerated(EnumType.STRING) @Column(name = "decision_body", length = 24) private gn.lacaeducation.discipline.domain.DecisionBody decisionBody;
}
