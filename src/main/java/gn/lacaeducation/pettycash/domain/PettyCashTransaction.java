package gn.lacaeducation.pettycash.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une opération de petite caisse (bon de sortie ou réapprovisionnement). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PettyCashTransaction {
    private Long id;
    private Long tenantId;
    private Long fundId;
    private LocalDate txDate;
    private PettyCashTxType txType;
    private BigDecimal amount;
    private String voucherNumber;
    private String description;
    private String category;
    private String recipient;
    private String justificationUrl;
    private Long recordedBy;
    private LocalDateTime createdAt;
}
