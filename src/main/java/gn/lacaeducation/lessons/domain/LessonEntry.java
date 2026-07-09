package gn.lacaeducation.lessons.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une entrée du cahier de textes : une séance pour une classe/matière, son contenu et les devoirs. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonEntry {
    private Long id;
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherId;
    private LocalDate lessonDate;
    private String title;
    private String content;
    private String homework;
    private Integer durationMinutes;
    private LessonStatus status;
    /** Id de l'évaluation source quand la séance a été générée automatiquement à la création d'une évaluation. */
    private Long sourceAssessmentId;
    private LocalDateTime createdAt;
}
