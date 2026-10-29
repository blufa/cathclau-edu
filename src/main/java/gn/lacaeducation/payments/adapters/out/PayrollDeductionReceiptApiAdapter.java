package gn.lacaeducation.payments.adapters.out;

import gn.lacaeducation.payments.PayrollDeductionReceiptApi;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
class PayrollDeductionReceiptApiAdapter implements PayrollDeductionReceiptApi {

    private final PaymentUseCase paymentUseCase;

    @Override
    public Long recordForReceipt(Long tenantId, Long invoiceId, Long studentId, Long academicYearId, BigDecimal amount) {
        Payment payment = paymentUseCase.recordSystemPayment(new PaymentUseCase.SystemPaymentCommand(
                tenantId, invoiceId, studentId, academicYearId, PaymentChannel.PAYROLL_DEDUCTION, amount));
        return payment.getId();
    }
}
