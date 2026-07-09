package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TreasuryAccount {
    private Long id;
    private Long tenantId;
    private String label;
    private TreasuryAccountType type;
    private String bank;
    private String accountNumber;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private String currency;
    private Boolean active;
}
