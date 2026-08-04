package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.expenses.application.port.out.BudgetRepository;
import gn.lacaeducation.expenses.domain.Budget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock BudgetRepository repository;
    @InjectMocks BudgetService service;

    @Test
    void save_startsWithZeroConsumed() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Budget created = service.save(new BudgetService.BudgetCommand(1L, 10L, "2025-2026", BigDecimal.valueOf(1_000_000)));

        assertThat(created.getConsumedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.getRemainingBalance()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
    }

    @Test
    void onExpensePaid_decrementsBalanceOfMatchingCategoryBudget() {
        Budget budget = Budget.builder().id(5L).tenantId(1L).categoryId(10L)
                .allocatedAmount(BigDecimal.valueOf(1_000_000)).consumedAmount(BigDecimal.valueOf(200_000)).build();
        when(repository.findByTenantId(1L)).thenReturn(List.of(budget));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.onExpensePaid(new ExpensePaidEvent(1L, 42L, 10L, null, BigDecimal.valueOf(150_000)));

        assertThat(budget.getConsumedAmount()).isEqualByComparingTo(BigDecimal.valueOf(350_000));
        assertThat(budget.getRemainingBalance()).isEqualByComparingTo(BigDecimal.valueOf(650_000));
    }

    @Test
    void onExpensePaid_ignoresOtherCategories() {
        Budget budget = Budget.builder().id(5L).tenantId(1L).categoryId(10L)
                .allocatedAmount(BigDecimal.valueOf(1_000_000)).consumedAmount(BigDecimal.ZERO).build();
        when(repository.findByTenantId(1L)).thenReturn(List.of(budget));

        service.onExpensePaid(new ExpensePaidEvent(1L, 42L, 99L, null, BigDecimal.valueOf(150_000)));

        assertThat(budget.getConsumedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(repository, never()).save(any());
    }
}
