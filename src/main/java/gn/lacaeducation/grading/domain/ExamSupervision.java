package gn.lacaeducation.grading.domain;

import lombok.*;

/** Affectation d'un enseignant surveillant à une salle pour un examen (composition). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExamSupervision {
    private Long id;
    private Long tenantId;
    private Long examId;
    private Long roomId;
    private Long teacherId;
}
