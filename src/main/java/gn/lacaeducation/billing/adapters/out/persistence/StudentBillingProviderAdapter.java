package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.StudentBillingProvider;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentBillingProviderAdapter implements StudentBillingProvider {

    private final InvoiceUseCase invoiceUseCase;

    @Override
    public Optional<InvoiceInfo> latestInvoice(Long studentId) {
        return invoiceUseCase.findByStudentId(studentId).stream()
                .max(Comparator.comparing(Invoice::getIssueDate, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())))
                .map(this::toInfo);
    }

    @Override
    public Optional<InvoiceInfo> findInvoice(Long invoiceId) {
        return invoiceUseCase.findById(invoiceId).map(this::toInfo);
    }

    private InvoiceInfo toInfo(Invoice i) {
        return new InvoiceInfo(i.getStudentId(), i.getReference(), i.getTotalAmount(), i.getPaidAmount(),
                i.getRemainingBalance(), i.getStatus() != null ? i.getStatus().name() : null, i.getPeriodLabel());
    }
}
