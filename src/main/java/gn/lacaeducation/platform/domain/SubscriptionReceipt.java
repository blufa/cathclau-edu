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
public class SubscriptionReceipt {
    private Long id;
    private Long paymentId;
    private String reference;
    private BigDecimal amount;
    private String pdfFile;
    private LocalDateTime generatedAt;
}
