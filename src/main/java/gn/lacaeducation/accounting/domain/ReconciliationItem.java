package gn.lacaeducation.accounting.domain;

import lombok.*;
import java.math.BigDecimal;

/** Un élément en suspens d'un rapprochement bancaire. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReconciliationItem {
    private Long id;
    private Long tenantId;
    private Long reconciliationId;
    private ReconciliationItemType itemType;
    private String description;
    private BigDecimal amount;
}
