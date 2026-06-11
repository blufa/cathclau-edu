package gn.lacaeducation.payments.application.event;
public record PaymentSucceededEvent(Long tenantId, Long studentId, Long paymentId, Long invoiceId) {}
