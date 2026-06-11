package gn.lacaeducation.billing.application.event;
public record InvoiceGeneratedEvent(Long tenantId, Long studentId, Long invoiceId, String reference) {}
