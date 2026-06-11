package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.InvoiceDataProvider;
import gn.lacaeducation.billing.InvoicePublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class InvoiceDataProviderAdapter implements InvoiceDataProvider {
    private final InvoiceJpaRepository jpa;
    @Override
    public Optional<InvoicePublicInfo> findInvoice(Long invoiceId) {
        return jpa.findById(invoiceId).map(e ->
                new InvoicePublicInfo(e.getId(), e.getReference(), e.getTotalAmount(), e.getRemainingBalance(), e.getIssueDate()));
    }
    @Override
    public Optional<InvoicePublicInfo> findByReference(String reference) {
        return jpa.findByReference(reference).map(e ->
                new InvoicePublicInfo(e.getId(), e.getReference(), e.getTotalAmount(), e.getRemainingBalance(), e.getIssueDate()));
    }
}
