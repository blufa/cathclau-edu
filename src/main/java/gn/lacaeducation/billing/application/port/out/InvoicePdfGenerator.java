package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.Invoice;

/** Rend une facture scolaire en document PDF (octets). */
public interface InvoicePdfGenerator {
    byte[] generate(Invoice invoice);
}
