package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exemptions")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ExemptionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ExemptionType type;
    @Column(precision = 5, scale = 2) private BigDecimal percentage;
    @Column(precision = 12, scale = 2) private BigDecimal amount;
    @Convert(converter = FeeTypeListConverter.class) @Column(name = "fee_types") private List<FeeType> feeTypes;
    @Column(columnDefinition = "text", nullable = false) private String reason;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "validated_by") private Long validatedBy;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "staff_benefit", nullable = false) private boolean staffBenefit;
}
