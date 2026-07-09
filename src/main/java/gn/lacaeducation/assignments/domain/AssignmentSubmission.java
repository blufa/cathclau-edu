package gn.lacaeducation.assignments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Le rendu d'un élève pour un devoir : date de remise, fichier, note et appréciation. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmission {
    private Long id;
    private Long tenantId;
    private Long assignmentId;
    private Long studentId;
    private LocalDateTime submittedAt;
    private SubmissionStatus status;
    private String fileUrl;
    private BigDecimal grade;
    private String feedback;
}
