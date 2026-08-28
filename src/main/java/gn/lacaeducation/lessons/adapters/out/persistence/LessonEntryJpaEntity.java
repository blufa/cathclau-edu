package gn.lacaeducation.lessons.adapters.out.persistence;

import gn.lacaeducation.lessons.domain.LessonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_entries")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class LessonEntryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    /** Nullable : un enseignant unique (moniteur maternelle/primaire) consigne sa journée sans matière précise. */
    @Column(name = "subject_id") private Long subjectId;
    @Column(name = "teacher_id") private Long teacherId;
    @Column(name = "lesson_date", nullable = false) private LocalDate lessonDate;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String content;
    @Column(columnDefinition = "text") private String homework;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private LessonStatus status;
    @Column(name = "source_assessment_id") private Long sourceAssessmentId;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
