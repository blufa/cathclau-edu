package gn.lacaeducation.payments.application.event;
import java.math.BigDecimal;
public record PaymentSucceededEvent(Long tenantId, Long studentId, Long paymentId, Long invoiceId, BigDecimal amount) {}
