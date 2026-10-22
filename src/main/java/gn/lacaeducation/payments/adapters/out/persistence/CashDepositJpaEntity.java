package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "cash_deposits")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class CashDepositJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "accountant_id", nullable = false) private Long accountantId;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentChannel method;
    @Column(name = "destination_account", columnDefinition = "text") private String destinationAccount;
    @Column(name = "deposited_at", nullable = false) private LocalDateTime depositedAt;
    @Column(name = "transaction_reference", columnDefinition = "text") private String transactionReference;
    @Column(columnDefinition = "text") private String comment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CashDepositStatus status;
    @Column(name = "validated_by") private Long validatedBy;
    @Column(name = "validated_at") private LocalDateTime validatedAt;
    @Column(name = "rejection_reason", columnDefinition = "text") private String rejectionReason;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
