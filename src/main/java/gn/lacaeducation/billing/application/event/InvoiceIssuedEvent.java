package gn.lacaeducation.billing.application.event;

public record InvoiceIssuedEvent(Long invoiceId, Long studentId, Long tenantId) {}
