package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.CreateInvoiceCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listens to EnrollmentValidatedEvent and automatically creates an ENROLLMENT invoice
 * for the enrolled student when a school fee of type ENROLLMENT exists for that class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentInvoiceListener {

    private final SchoolFeeRepository schoolFeeRepository;
    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onEnrollmentValidated(EnrollmentValidatedEvent event) {
        List<SchoolFee> enrollmentFees = schoolFeeRepository.findByClassIdAndType(
                event.classId(), FeeType.ENROLLMENT);

        if (enrollmentFees.isEmpty()) {
            log.debug("No ENROLLMENT fee configured for class id={}, skipping invoice creation", event.classId());
            return;
        }

        for (SchoolFee fee : enrollmentFees) {
            // Skip if invoice already exists for this student+fee combination (idempotence)
            if (invoiceUseCase.existsByStudentIdAndFeeId(event.studentId(), fee.getId())) {
                log.debug("Invoice already exists for student id={} fee id={}, skipping", event.studentId(), fee.getId());
                continue;
            }

            String reference = generateReference(event.tenantId(), event.studentId(), fee.getId());

            invoiceUseCase.create(new CreateInvoiceCommand(
                    event.tenantId(),
                    event.studentId(),
                    fee.getId(),
                    reference,
                    fee.getAmount()
            ));

            log.info("Auto-created enrollment invoice for student id={} class id={} fee id={}",
                    event.studentId(), event.classId(), fee.getId());
        }
    }

    private String generateReference(Long tenantId, Long studentId, Long feeId) {
        // Format: INS-<tenantId>-<studentId>-<feeId>-<short-uuid>
        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("INS-%d-%d-%d-%s", tenantId, studentId, feeId, shortId);
    }
}
