package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_transfer_refunds")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ClassTransferRefundJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "old_class_id") private Long oldClassId;
    @Column(name = "new_class_id") private Long newClassId;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(length = 255) private String reason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "refunded_at") private LocalDateTime refundedAt;
}
