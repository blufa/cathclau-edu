package gn.lacaeducation.billing;
import java.util.Optional;
public interface InvoiceDataProvider {
    Optional<InvoicePublicInfo> findInvoice(Long invoiceId);
    Optional<InvoicePublicInfo> findByReference(String reference);
}
