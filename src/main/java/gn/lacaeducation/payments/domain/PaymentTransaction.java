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
    private LocalDateTime createdAt;
}
