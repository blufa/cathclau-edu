package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_credits")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class StudentCreditJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "remaining_amount", nullable = false, precision = 14, scale = 2) private BigDecimal remainingAmount;
    @Column(name = "source_payment_id") private Long sourcePaymentId;
    @Column(name = "source_invoice_id") private Long sourceInvoiceId;
    @Column(length = 255) private String reason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
