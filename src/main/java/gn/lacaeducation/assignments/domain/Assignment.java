package gn.lacaeducation.assignments.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un devoir donné à une classe pour une matière, avec une échéance de remise. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    private Long id;
    private Long tenantId;
    private Long classId;
    private Long subjectId;
    private Long teacherId;
    private String title;
    private String instructions;
    private String attachmentUrl;
    private LocalDate dueDate;
    private BigDecimal maxScore;
    private AssignmentStatus status;
    private LocalDateTime createdAt;
}
