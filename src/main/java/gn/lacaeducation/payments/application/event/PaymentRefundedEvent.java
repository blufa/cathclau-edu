package gn.lacaeducation.payments.application.event;
import java.math.BigDecimal;
/** PaiementRembourse (§9.1): payment refunded; downstream adjusts the invoice and issues a credit receipt. */
public record PaymentRefundedEvent(Long tenantId, Long studentId, Long paymentId, Long invoiceId, BigDecimal amount) {}
