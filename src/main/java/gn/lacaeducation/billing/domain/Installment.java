package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Installment {
    private Long id;
    private Long feeId;
    private String label;
    private BigDecimal amount;
    private LocalDate dueDate;
}
