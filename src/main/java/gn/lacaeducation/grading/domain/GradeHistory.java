package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit entry tracing a single change to a {@link Grade}: what changed
 * (old value → new value), which action, who made it and (optionally) why. Lets the
 * school understand a grade's revision history — "pourquoi tant de modifications".
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradeHistory {
    private Long id;
    private Long gradeId;
    private Long assessmentId;
    private Long studentId;
    private GradeAction action;
    private BigDecimal oldValue;
    private BigDecimal newValue;
    /** Id of the user (teacher/admin) who performed the change; null when unauthenticated/system. */
    private Long changedBy;
    /** Free-text justification supplied on correction. */
    private String reason;
    private LocalDateTime changedAt;
}
