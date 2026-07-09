package gn.lacaeducation.payments.application.port.out;

import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.Receipt;

/** Rend un reçu de paiement en document PDF (octets). */
public interface ReceiptPdfGenerator {
    byte[] generate(Receipt receipt, Payment payment);
}
