package gn.lacaeducation.accounting.domain;

import lombok.*;

/** A chart-of-accounts line (plan comptable SYSCOHADA). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChartOfAccount {
    private Long id;
    private Long tenantId;
    private String accountNumber;   // e.g. 411, 521, 701
    private String label;
    private Integer accountClass;   // 1..8
    private AccountType type;
    private Long parentId;
    private Boolean lettrable;
    private Boolean active;
}
