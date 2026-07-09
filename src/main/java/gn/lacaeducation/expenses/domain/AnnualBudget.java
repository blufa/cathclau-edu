package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Budget annuel de l'école (recettes + dépenses prévisionnelles), Manuel §3. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnualBudget {
    private Long id;
    private Long tenantId;
    private String academicYear;
    private String label;
    private BudgetStatus status;
    private Long approvedBy;
    private LocalDate approvedAt;
    private String rejectionReason;
    private String notes;
    private LocalDateTime createdAt;
}
