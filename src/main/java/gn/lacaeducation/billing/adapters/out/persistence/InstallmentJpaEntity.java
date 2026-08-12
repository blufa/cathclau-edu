package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Entity @Table(name = "installments") @Getter @Setter
public class InstallmentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "fee_id", nullable = false) private Long feeId;
    @Column(nullable = false) private String label;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InstallmentStatus status;
    @Convert(converter = InstallmentMonthsConverter.class)
    @Column(name = "months")
    private List<String> months;
}
