package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_credit_usages")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class StudentCreditUsageJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "credit_id", nullable = false) private Long creditId;
    @Column(name = "target_invoice_id", nullable = false) private Long targetInvoiceId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "applied_by_user_id") private Long appliedByUserId;
    @Column(name = "applied_at", nullable = false) private LocalDateTime appliedAt;
}
