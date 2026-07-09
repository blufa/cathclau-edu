package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Agrège la facturation d'un tenant pour le reporting financier (§10.2). */
@Component
@RequiredArgsConstructor
class SchoolBillingStatsProviderAdapter implements SchoolBillingStatsProvider {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional(readOnly = true)
    public BillingStats billingStats(Long tenantId) {
        List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);
        BigDecimal invoiced = sum(invoices, Invoice::getTotalAmount);
        BigDecimal collected = sum(invoices, Invoice::getPaidAmount);
        BigDecimal outstanding = sum(invoices, Invoice::getRemainingBalance);
        BigDecimal recoveryRate = invoiced.signum() == 0 ? BigDecimal.ZERO
                : collected.multiply(HUNDRED).divide(invoiced, 2, RoundingMode.HALF_UP);
        long unpaidCount = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .count();
        return new BillingStats(invoiced, collected, outstanding, recoveryRate, unpaidCount);
    }

    private BigDecimal sum(List<Invoice> invoices, java.util.function.Function<Invoice, BigDecimal> field) {
        return invoices.stream()
                .map(field)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
