package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.students.ArrearsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
class ArrearsPortAdapter implements ArrearsPort {

    private static final Set<InvoiceStatus> UNSETTLED = Set.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL);

    private final InvoiceUseCase invoiceUseCase;

    @Override
    public ArrearsInfo arrears(Long studentId, Long excludingAcademicYearId) {
        var unsettled = invoiceUseCase.findByStudentId(studentId).stream()
                .filter(i -> !Objects.equals(i.getAcademicYearId(), excludingAcademicYearId))
                .filter(i -> UNSETTLED.contains(i.getStatus()))
                .toList();
        BigDecimal total = unsettled.stream()
                .map(i -> i.getRemainingBalance() != null ? i.getRemainingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ArrearsInfo(total, unsettled.size());
    }
}
