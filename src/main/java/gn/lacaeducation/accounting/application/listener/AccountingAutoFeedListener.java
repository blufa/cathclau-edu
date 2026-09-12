package gn.lacaeducation.accounting.application.listener;

import gn.lacaeducation.accounting.application.service.TreasuryService;
import gn.lacaeducation.accounting.domain.CashFlowCategory;
import gn.lacaeducation.accounting.domain.CashFlowDirection;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payroll.application.event.PayslipPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Feeds the SIMPLE-level cash book from other modules' events (no data duplicated — only an aggregated
 * cash-flow line): a successful payment is an IN, a paid expense is an OUT. Idempotent on (refType, refId).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountingAutoFeedListener {

    private final TreasuryService treasuryService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSucceeded(PaymentSucceededEvent e) {
        treasuryService.recordAuto(e.tenantId(), CashFlowDirection.IN, e.amount(), CashFlowCategory.TUITION,
                CashFlowReferenceType.PAYMENT, e.paymentId(), "Encaissement paiement #" + e.paymentId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpensePaid(ExpensePaidEvent e) {
        treasuryService.recordAuto(e.tenantId(), CashFlowDirection.OUT, e.amount(), CashFlowCategory.SUPPLIER,
                CashFlowReferenceType.EXPENSE, e.expenseId(), "Règlement dépense #" + e.expenseId());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPayslipPaid(PayslipPaidEvent e) {
        treasuryService.recordAuto(e.tenantId(), CashFlowDirection.OUT, e.amount(), CashFlowCategory.SALARY,
                CashFlowReferenceType.PAYSLIP, e.payslipId(), "Règlement fiche de paie #" + e.payslipId());
    }
}
