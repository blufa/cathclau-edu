package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un état de rapprochement bancaire pour un compte de trésorerie à une date de relevé. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BankReconciliation {
    private Long id;
    private Long tenantId;
    private Long treasuryAccountId;
    private LocalDate statementDate;
    private BigDecimal statementBalance;
    private BigDecimal bookBalance;
    private ReconciliationStatus status;
    private String notes;
    private Long reconciledBy;
    private LocalDateTime createdAt;
}
