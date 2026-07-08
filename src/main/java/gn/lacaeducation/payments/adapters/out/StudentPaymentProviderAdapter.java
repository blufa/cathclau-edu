package gn.lacaeducation.payments.adapters.out;

import gn.lacaeducation.payments.StudentPaymentProvider;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentPaymentProviderAdapter implements StudentPaymentProvider {

    private final PaymentUseCase paymentUseCase;

    @Override
    public Optional<PaymentInfo> latestSucceeded(Long studentId) {
        return paymentUseCase.findByStudentId(studentId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .max(Comparator.comparing(Payment::getPaidAt, Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())))
                .map(this::toInfo);
    }

    @Override
    public Optional<PaymentInfo> findPayment(Long paymentId) {
        return paymentUseCase.findById(paymentId).map(this::toInfo);
    }

    private PaymentInfo toInfo(Payment p) {
        return new PaymentInfo(p.getStudentId(), p.getInvoiceId(), p.getReference(), p.getAmount(),
                p.getChannel() != null ? p.getChannel().name() : null,
                p.getStatus() != null ? p.getStatus().name() : null, p.getPaidAt());
    }
}
