package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CashFlowEntry {
    private Long id;
    private Long tenantId;
    private Long accountId;
    private CashFlowDirection direction;
    private BigDecimal amount;
    private String currency;
    private LocalDate date;
    private CashFlowCategory category;
    private String label;
    private CashFlowReferenceType referenceType;
    private Long referenceId;
    private String justificationUrl;
    private Boolean reconciled;
}
