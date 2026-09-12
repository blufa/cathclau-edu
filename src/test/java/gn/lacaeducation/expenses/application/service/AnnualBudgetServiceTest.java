package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.expenses.application.port.out.AnnualBudgetPdfGenerator;
import gn.lacaeducation.expenses.application.port.out.AnnualBudgetRepository;
import gn.lacaeducation.expenses.application.port.out.BudgetLineRepository;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;
import gn.lacaeducation.expenses.domain.BudgetLineType;
import gn.lacaeducation.expenses.domain.BudgetStatus;
import gn.lacaeducation.expenses.domain.FinancingMode;
import gn.lacaeducation.common.MediaUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualBudgetServiceTest {

    @Mock AnnualBudgetRepository budgetRepo;
    @Mock BudgetLineRepository lineRepo;
    @Mock AnnualBudgetPdfGenerator pdfGenerator;
    @Mock MediaUploadService mediaUploadService;
    @InjectMocks AnnualBudgetService service;

    @Test
    void create_defaultsFinancingModeToNone_whenNotProvided() {
        when(budgetRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AnnualBudget b = service.create(new AnnualBudgetService.BudgetCommand(
                1L, "2025-2026", "Budget kermesse", null, null, null));

        assertThat(b.getFinancingMode()).isEqualTo(FinancingMode.NONE);
        assertThat(b.getStatus()).isEqualTo(BudgetStatus.DRAFT);
    }

    @Test
    void create_storesFinancingModeAndAmount_whenProvided() {
        when(budgetRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AnnualBudget b = service.create(new AnnualBudgetService.BudgetCommand(
                1L, "2025-2026", "Budget kermesse", null, FinancingMode.COLLECTE, BigDecimal.valueOf(500_000)));

        assertThat(b.getFinancingMode()).isEqualTo(FinancingMode.COLLECTE);
        assertThat(b.getFinancingAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void addLine_computesPlannedAmount_fromQuantityTimesUnitPrice() {
        AnnualBudget budget = AnnualBudget.builder().id(1L).tenantId(1L).status(BudgetStatus.DRAFT).build();
        when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));
        when(lineRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        BudgetLine line = service.addLine(1L, new AnnualBudgetService.LineCommand(
                BudgetLineType.EXPENSE, "Chaises à louer", 50, BigDecimal.valueOf(2_000), null, null, null));

        assertThat(line.getPlannedAmount()).isEqualByComparingTo("100000");
        assertThat(line.getQuantity()).isEqualTo(50);
        assertThat(line.getUnitPrice()).isEqualByComparingTo("2000");
    }

    @Test
    void addLine_usesPlannedAmountDirectly_whenNoQuantity() {
        AnnualBudget budget = AnnualBudget.builder().id(1L).tenantId(1L).status(BudgetStatus.DRAFT).build();
        when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));
        when(lineRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        BudgetLine line = service.addLine(1L, new AnnualBudgetService.LineCommand(
                BudgetLineType.EXPENSE, "Location de la salle", null, null, BigDecimal.valueOf(300_000), null, null));

        assertThat(line.getPlannedAmount()).isEqualByComparingTo("300000");
        assertThat(line.getQuantity()).isNull();
    }

    @Test
    void pdfUrl_generatesAndCaches_whenNotAlreadyGenerated() {
        AnnualBudget budget = AnnualBudget.builder().id(1L).tenantId(1L).label("Budget kermesse")
                .status(BudgetStatus.DRAFT).build();
        when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));
        when(lineRepo.findByBudgetId(1L)).thenReturn(List.of());
        when(pdfGenerator.generate(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(mediaUploadService.uploadDocument(any(), any(), any())).thenReturn("https://cdn.test/budget-1.pdf");
        when(budgetRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        String url = service.pdfUrl(1L);

        assertThat(url).isEqualTo("https://cdn.test/budget-1.pdf");
        assertThat(budget.getPdfFile()).isEqualTo("https://cdn.test/budget-1.pdf");
    }

    @Test
    void onExpensePaid_incrementsLinkedLineActualAmount() {
        BudgetLine line = BudgetLine.builder().id(5L).tenantId(1L).budgetId(1L)
                .lineType(BudgetLineType.EXPENSE).category("Craie").actualAmount(BigDecimal.valueOf(20_000)).build();
        when(lineRepo.findById(5L)).thenReturn(Optional.of(line));
        when(lineRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.onExpensePaid(new ExpensePaidEvent(1L, 42L, 3L, 5L, BigDecimal.valueOf(15_000)));

        assertThat(line.getActualAmount()).isEqualByComparingTo("35000");
    }

    @Test
    void onExpensePaid_withoutBudgetLineId_doesNothing() {
        service.onExpensePaid(new ExpensePaidEvent(1L, 42L, 3L, null, BigDecimal.valueOf(15_000)));

        org.mockito.Mockito.verifyNoInteractions(lineRepo);
    }

    @Test
    void pdfUrl_returnsCachedUrl_whenAlreadyGenerated() {
        AnnualBudget budget = AnnualBudget.builder().id(1L).tenantId(1L).label("Budget kermesse")
                .status(BudgetStatus.DRAFT).pdfFile("https://cdn.test/existing.pdf").build();
        when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));

        String url = service.pdfUrl(1L);

        assertThat(url).isEqualTo("https://cdn.test/existing.pdf");
    }
}
