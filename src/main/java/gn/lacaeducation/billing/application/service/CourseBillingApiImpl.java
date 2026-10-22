package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.CourseBillingApi;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.SchoolFeeUseCase;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implémentation de {@link CourseBillingApi} : délègue aux ports internes du module ({@link
 * SchoolFeeUseCase}, {@link InvoiceUseCase}), jamais exposés hors de {@code billing}.
 */
@Service
@RequiredArgsConstructor
class CourseBillingApiImpl implements CourseBillingApi {

    private final SchoolFeeUseCase schoolFeeUseCase;
    private final InvoiceUseCase invoiceUseCase;

    @Override
    public Long createCourseFee(Long tenantId, Long classId, BigDecimal amount, String academicYear, String label) {
        SchoolFee fee = schoolFeeUseCase.create(new SchoolFeeUseCase.CreateFeeCommand(
                tenantId, classId, null, FeeType.OTHER, amount, academicYear, label, null), null);
        return fee.getId();
    }

    @Override
    public void updateCourseFeeAmount(Long feeId, BigDecimal amount, String label) {
        SchoolFee fee = schoolFeeUseCase.findById(feeId)
                .orElseThrow(() -> new ResourceNotFoundException("SchoolFee", feeId));
        schoolFeeUseCase.update(feeId, new SchoolFeeUseCase.UpdateFeeCommand(
                fee.getClassId(), fee.getRouteId(), fee.getType(), amount, fee.getAcademicYear(), label, fee.getMandatory()));
    }

    @Override
    public Long issueCourseInvoice(Long tenantId, Long studentId, Long feeId, Long academicYearId, BigDecimal amount) {
        Invoice invoice = invoiceUseCase.create(new InvoiceUseCase.CreateInvoiceCommand(
                tenantId, studentId, feeId, null, amount, academicYearId));
        return invoice.getId();
    }
}
