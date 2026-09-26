package gn.lacaeducation.expenses.adapters.out.persistence.mapper;

import gn.lacaeducation.expenses.adapters.out.persistence.AnnualBudgetJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.BudgetLineJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.ExpenseJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.MonthlyExpenseSheetJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.RevenueLineJpaEntity;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import gn.lacaeducation.expenses.domain.RevenueLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpensesMapper {
    Expense toDomain(ExpenseJpaEntity e);
    ExpenseJpaEntity toEntity(Expense d);

    MonthlyExpenseSheet toDomain(MonthlyExpenseSheetJpaEntity e);
    MonthlyExpenseSheetJpaEntity toEntity(MonthlyExpenseSheet d);

    RevenueLine toDomain(RevenueLineJpaEntity e);
    RevenueLineJpaEntity toEntity(RevenueLine d);

    AnnualBudget toDomain(AnnualBudgetJpaEntity e);
    AnnualBudgetJpaEntity toEntity(AnnualBudget d);

    BudgetLine toDomain(BudgetLineJpaEntity e);
    BudgetLineJpaEntity toEntity(BudgetLine d);
}
