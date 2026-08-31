package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_fee_settlements")
@Getter
@Setter
@NoArgsConstructor
@Filter(name = "tenantFilter")
public class PlatformFeeSettlementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
