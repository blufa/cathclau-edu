package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment {
    private Long id;
    private Long invoiceId;
    private String reference;
    private BigDecimal amount;
    private PaymentChannel channel;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String externalTransactionId;
    /** Djomy aggregator redirect URL returned to the frontend (null for non-aggregator channels). */
    private String redirectUrl;
    private LocalDateTime paidAt;
}
