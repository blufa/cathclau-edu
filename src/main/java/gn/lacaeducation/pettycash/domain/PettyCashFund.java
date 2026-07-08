package gn.lacaeducation.pettycash.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Une caisse de menues dépenses à fonds fixe. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PettyCashFund {
    private Long id;
    private Long tenantId;
    private String label;
    private BigDecimal floatAmount;
    private BigDecimal currentBalance;
    private String custodian;
    private Boolean active;
    private LocalDateTime createdAt;
}
