package gn.lacaeducation.accounting.application.listener;

import gn.lacaeducation.accounting.application.service.TreasuryService;
import gn.lacaeducation.accounting.domain.CashFlowCategory;
import gn.lacaeducation.accounting.domain.CashFlowDirection;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payroll.application.event.PayslipPaidEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountingAutoFeedListenerTest {

    @Mock TreasuryService treasuryService;
    @InjectMocks AccountingAutoFeedListener listener;

    @Test
    void onExpensePaid_recordsOutCashFlow_categorySupplier() {
        listener.onExpensePaid(new ExpensePaidEvent(1L, 42L, 3L, 5L, BigDecimal.valueOf(15_000)));

        verify(treasuryService).recordAuto(1L, CashFlowDirection.OUT, BigDecimal.valueOf(15_000),
                CashFlowCategory.SUPPLIER, CashFlowReferenceType.EXPENSE, 42L, "Règlement dépense #42");
    }

    /** La paie fait désormais remonter une sortie de caisse "Salaires" — jusqu'ici invisible de la
     * trésorerie, contrairement aux dépenses classiques. */
    @Test
    void onPayslipPaid_recordsOutCashFlow_categorySalary() {
        listener.onPayslipPaid(new PayslipPaidEvent(1L, 99L, BigDecimal.valueOf(850_000)));

        verify(treasuryService).recordAuto(1L, CashFlowDirection.OUT, BigDecimal.valueOf(850_000),
                CashFlowCategory.SALARY, CashFlowReferenceType.PAYSLIP, 99L, "Règlement fiche de paie #99");
    }

    @Test
    void onPaymentSucceeded_recordsInCashFlow_categoryTuition() {
        listener.onPaymentSucceeded(new PaymentSucceededEvent(1L, 999L, 55L, 20L, BigDecimal.valueOf(100_000)));

        verify(treasuryService).recordAuto(1L, CashFlowDirection.IN, BigDecimal.valueOf(100_000),
                CashFlowCategory.TUITION, CashFlowReferenceType.PAYMENT, 55L, "Encaissement paiement #55");
    }
}
