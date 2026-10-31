package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.application.port.out.DuplicateDetectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class DuplicateDetectionAdapter implements DuplicateDetectionPort {

    private final DuplicateStudentJpaRepository jpa;

    @Override
    public List<StudentFinancialRow> findDuplicateCandidates(Long tenantId) {
        return jpa.findDuplicateCandidates(tenantId).stream()
                .map(r -> new StudentFinancialRow(r.getStudentId(), r.getFirstName(), r.getLastName(), r.getPhoto(),
                        r.getClassId(), r.getClassName(), r.getTotalInvoiced(), r.getTotalPaid(),
                        r.getMonthlyInvoiceCount() == null ? 0L : r.getMonthlyInvoiceCount()))
                .toList();
    }
}
