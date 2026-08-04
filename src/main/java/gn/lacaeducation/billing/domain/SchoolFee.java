package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SchoolFee {
    private Long id;
    private Long tenantId;
    private Long classId;
    private FeeType type;
    private BigDecimal amount;
    private String academicYear;
    /** Nom de la catégorie quand {@code type == OTHER} (ex. "Tenue scolaire", "Bus de transport"). */
    private String label;
}
