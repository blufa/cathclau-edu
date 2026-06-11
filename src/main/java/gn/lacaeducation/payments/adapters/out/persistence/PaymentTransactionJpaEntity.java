package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.domain.PaymentProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "payment_transactions") @Getter @Setter
public class PaymentTransactionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentProvider provider;
    @Column(name = "external_transaction_id") private String externalTransactionId;
    @Column(name = "provider_status") private String providerStatus;
    @Lob @Column(name = "response_payload") private String responsePayload;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
