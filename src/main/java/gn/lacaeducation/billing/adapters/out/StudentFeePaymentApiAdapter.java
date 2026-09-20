package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.StudentFeePaymentApi;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
class StudentFeePaymentApiAdapter implements StudentFeePaymentApi {

    private final InvoiceUseCase invoiceUseCase;

    @Override
    public void applyMonthlyPayment(Long tenantId, Long studentId, BigDecimal amount) {
        invoiceUseCase.applyExternalPayment(tenantId, studentId, amount);
    }

    @Override
    public BigDecimal outstandingStaffBenefitRemainder(Long studentId) {
        return invoiceUseCase.outstandingStaffBenefitRemainder(studentId);
    }
}
