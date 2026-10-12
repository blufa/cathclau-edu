package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.payments.EnrollmentFeeGateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class EnrollmentFeeGateAdapter implements EnrollmentFeeGateProvider {

    private final InvoiceRepository invoiceRepository;
    private final SchoolFeeRepository schoolFeeRepository;

    @Override
    public List<Long> outstandingEnrollmentInvoiceIds(Long studentId) {
        if (studentId == null) return List.of();
        return invoiceRepository.findByStudentId(studentId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .filter(i -> i.getFeeId() != null)
                .filter(i -> schoolFeeRepository.findById(i.getFeeId())
                        .map(f -> f.getType() == FeeType.ENROLLMENT || f.getType() == FeeType.RE_ENROLLMENT)
                        .orElse(false))
                .map(Invoice::getId)
                .toList();
    }
}
