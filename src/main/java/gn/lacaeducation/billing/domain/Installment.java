package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Installment {
    private Long id;
    private Long feeId;
    private String label;
    private BigDecimal amount;
    private LocalDate dueDate;
    private InstallmentStatus status;
    /** Mois couverts par cette tranche ("YYYY-MM"), librement choisis — pas forcément consécutifs. */
    private List<String> months;
}
