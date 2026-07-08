package gn.lacaeducation.expenses.adapters.out.persistence.mapper;

import gn.lacaeducation.expenses.adapters.out.persistence.AnnualBudgetJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.BudgetJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.BudgetLineJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.ExpenseCategoryJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.ExpenseJpaEntity;
import gn.lacaeducation.expenses.adapters.out.persistence.SupplierJpaEntity;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.Budget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.ExpenseCategory;
import gn.lacaeducation.expenses.domain.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpensesMapper {
    Expense toDomain(ExpenseJpaEntity e);
    ExpenseJpaEntity toEntity(Expense d);

    Supplier toDomain(SupplierJpaEntity e);
    SupplierJpaEntity toEntity(Supplier d);

    ExpenseCategory toDomain(ExpenseCategoryJpaEntity e);
    ExpenseCategoryJpaEntity toEntity(ExpenseCategory d);

    Budget toDomain(BudgetJpaEntity e);
    BudgetJpaEntity toEntity(Budget d);

    AnnualBudget toDomain(AnnualBudgetJpaEntity e);
    AnnualBudgetJpaEntity toEntity(AnnualBudget d);

    BudgetLine toDomain(BudgetLineJpaEntity e);
    BudgetLineJpaEntity toEntity(BudgetLine d);
}
