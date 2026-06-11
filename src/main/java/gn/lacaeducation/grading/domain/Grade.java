package gn.lacaeducation.grading.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Grade {
    private Long id;
    private Long assessmentId;
    private Long studentId;
    private BigDecimal value;
    private Boolean validated;
    private LocalDateTime enteredAt;
}
