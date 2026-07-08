package gn.lacaeducation.grants.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un versement reçu au titre d'une subvention. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GrantPayment {
    private Long id;
    private Long tenantId;
    private Long grantId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String method;
    private String reference;
    private Long treasuryAccountId;
    private String notes;
    private LocalDateTime createdAt;
}
