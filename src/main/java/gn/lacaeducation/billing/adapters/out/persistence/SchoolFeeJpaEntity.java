package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "school_fees") @Getter @Setter
public class SchoolFeeJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id") private Long classId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FeeType type;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "academic_year", nullable = false) private String academicYear;
}
