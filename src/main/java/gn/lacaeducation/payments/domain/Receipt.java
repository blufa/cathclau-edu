package gn.lacaeducation.payments.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Receipt {
    private Long id;
    private Long paymentId;
    private String reference;
    private BigDecimal amount;
    private String pdfFile;
    private LocalDateTime generatedAt;
}
