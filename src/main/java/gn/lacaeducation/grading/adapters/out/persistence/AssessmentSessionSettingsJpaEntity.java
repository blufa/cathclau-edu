package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "assessment_session_settings")
@Getter @Setter @NoArgsConstructor
class AssessmentSessionSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Column(name = "quiz_minutes", nullable = false) private Integer quizMinutes;
    @Column(name = "homework_minutes", nullable = false) private Integer homeworkMinutes;
    @Column(name = "exam_minutes", nullable = false) private Integer examMinutes;
    @Column(name = "final_minutes", nullable = false) private Integer finalMinutes;
}
