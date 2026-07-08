package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.FiscalYearStatus;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name = "fiscal_year") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class FiscalYearJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private FiscalYearStatus status;
    @Column(name = "academic_year_id") private Long academicYearId;
}
