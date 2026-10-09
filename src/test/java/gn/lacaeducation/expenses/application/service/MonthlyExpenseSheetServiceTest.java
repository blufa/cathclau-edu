package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.expenses.application.port.out.ExpenseRepository;
import gn.lacaeducation.expenses.application.port.out.MonthlyExpenseSheetRepository;
import gn.lacaeducation.expenses.application.port.out.RevenueLineRepository;
import gn.lacaeducation.expenses.application.service.MonthlyExpenseSheetService.ExpenseLineCommand;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import gn.lacaeducation.payments.CashierSessionStatusProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyExpenseSheetServiceTest {

    @Mock MonthlyExpenseSheetRepository sheetRepo;
    @Mock ExpenseRepository expenseRepo;
    @Mock RevenueLineRepository revenueRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CashierSessionStatusProvider cashierSessionStatusProvider;

    private MonthlyExpenseSheetService service() {
        return new MonthlyExpenseSheetService(sheetRepo, expenseRepo, revenueRepo, eventPublisher, cashierSessionStatusProvider);
    }

    private final MonthlyExpenseSheet openSheet = MonthlyExpenseSheet.builder().id(1L).tenantId(10L).build();
    private final ExpenseLineCommand cmd = new ExpenseLineCommand("Fournitures", new BigDecimal("50000"), true, LocalDate.of(2026, 10, 5));

    @Test
    void addExpense_withoutOpenCashierSession_throwsBusinessRule() {
        when(sheetRepo.findById(1L)).thenReturn(Optional.of(openSheet));
        when(cashierSessionStatusProvider.hasOpenSession(99L)).thenReturn(false);

        assertThatThrownBy(() -> service().addExpense(1L, cmd, 99L)).isInstanceOf(BusinessRuleException.class);
        verify(expenseRepo, org.mockito.Mockito.never()).save(any(Expense.class));
    }

    @Test
    void addExpense_withOpenCashierSession_succeeds() {
        when(sheetRepo.findById(1L)).thenReturn(Optional.of(openSheet));
        when(cashierSessionStatusProvider.hasOpenSession(99L)).thenReturn(true);
        when(expenseRepo.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        Expense saved = service().addExpense(1L, cmd, 99L);

        assertThat(saved.getAmount()).isEqualByComparingTo("50000");
    }
}
