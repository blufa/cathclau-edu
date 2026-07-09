package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Expense category (self-referencing tree via parentId). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExpenseCategory {
    private Long id;
    private Long tenantId;
    private String name;
    private Long parentId;
}
