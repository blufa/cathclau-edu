package gn.lacaeducation.payments.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    private Long id;
    private Long tenantId;
    private Long invoiceId;
    private Long studentId;
    private String reference;
    private BigDecimal amount;
    private PaymentChannel channel;
    private PaymentType type;
    private PaymentStatus status;
    private LocalDateTime paidAt;
}
