package gn.lacaeducation.billing.application.event;

/**
 * Published when an invoice is fully settled (FacturePayee, §8.3).
 * Signals closure of the invoice so downstream modules (receipts via payments,
 * parent notification, outgoing webhooks) can react.
 */
public record InvoicePaidEvent(Long tenantId, Long studentId, Long invoiceId, String reference) {}
