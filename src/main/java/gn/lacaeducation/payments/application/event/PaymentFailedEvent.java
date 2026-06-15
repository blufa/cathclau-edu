package gn.lacaeducation.payments.application.event;
import java.math.BigDecimal;
/** PaiementEchoue (§9.1): payment failed; the invoice is left unchanged, parent is informed. */
public record PaymentFailedEvent(Long tenantId, Long studentId, Long paymentId, Long invoiceId, BigDecimal amount) {}
