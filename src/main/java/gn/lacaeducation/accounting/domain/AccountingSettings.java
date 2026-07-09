package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountingSettings {
    private Long id;
    private Long tenantId;
    private AccountingMode mode;
    private String referential;          // SYSCOHADA | OTHER
    private String currency;
    private Integer fiscalYearStartMonth; // 1..12 (e.g. 9 = September)
    private Boolean vatApplicable;
    private BigDecimal defaultVatRate;
    private Boolean autoNumbering;
    private Boolean active;
}
