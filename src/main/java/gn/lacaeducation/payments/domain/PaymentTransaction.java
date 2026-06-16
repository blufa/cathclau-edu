package gn.lacaeducation.payments.domain;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransaction {
    private Long id;
    private Long paymentId;
    private PaymentProvider provider;
    private String externalTransactionId;
    private String providerStatus;
    private String responsePayload;
    /** Aggregator redirect URL (Djomy create_payment_gateway) returned to the frontend; null otherwise. */
    private String redirectUrl;
    private LocalDateTime createdAt;
}
