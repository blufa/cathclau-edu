package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.students.EnrollmentValidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * When an ENROLLMENT-fee invoice is fully settled, validates the matching PENDING enrollment.
 * This makes enrollment validation payment-driven: the student is validated only once the
 * enrollment fee is entirely paid (partial payments leave the enrollment PENDING).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentValidationOnPaymentListener {

    private final InvoiceRepository invoiceRepository;
    private final SchoolFeeRepository schoolFeeRepository;
    private final EnrollmentValidationApi enrollmentValidationApi;

    @EventListener
    @Transactional
    public void onInvoicePaid(InvoicePaidEvent event) {
        invoiceRepository.findById(event.invoiceId()).ifPresent(invoice -> {
            if (invoice.getFeeId() == null) return;
            schoolFeeRepository.findById(invoice.getFeeId()).ifPresent(fee -> {
                if (fee.getType() != FeeType.ENROLLMENT) return;
                enrollmentValidationApi.validatePaidEnrollment(event.studentId(), fee.getClassId());
                log.info("Enrollment fee fully paid (invoice id={}): validated enrollment for student id={} class id={}",
                        event.invoiceId(), event.studentId(), fee.getClassId());
            });
        });
    }
}
