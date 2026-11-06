package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase;
import gn.lacaeducation.billing.application.port.out.ClassTransferRefundRepository;
import gn.lacaeducation.billing.domain.ClassTransferRefund;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClassTransferRefundService implements ClassTransferRefundUseCase {

    private final ClassTransferRefundRepository repo;
    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;

    @Override
    public ClassTransferRefund record(Long tenantId, Long studentId, Long oldClassId, Long newClassId,
                                       BigDecimal amount, String reason) {
        return repo.save(ClassTransferRefund.builder()
                .tenantId(tenantId).studentId(studentId).oldClassId(oldClassId).newClassId(newClassId)
                .amount(amount).reason(reason).createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassTransferRefund> findByStudentId(Long studentId) {
        return repo.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(ClassTransferRefund::getCreatedAt).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassTransferRefund> findPendingByStudentId(Long studentId) {
        return findByStudentId(studentId).stream().filter(ClassTransferRefund::isPending).toList();
    }

    @Override
    public ClassTransferRefund markRefunded(Long id) {
        ClassTransferRefund refund = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassTransferRefund", id));
        refund.setRefundedAt(LocalDateTime.now());
        return repo.save(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingRefund> findAllPending() {
        return repo.findPending().stream().map(this::toPending).toList();
    }

    private PendingRefund toPending(ClassTransferRefund r) {
        var student = studentDataProvider.findStudent(r.getStudentId()).orElse(null);
        String studentName = student != null ? (student.firstName() + " " + student.lastName()).trim() : "Élève #" + r.getStudentId();
        String studentNumber = student != null ? student.studentNumber() : null;
        String oldClassName = r.getOldClassId() != null
                ? academicsDataProvider.findClass(r.getOldClassId()).map(gn.lacaeducation.academics.ClassPublicInfo::label).orElse(null)
                : null;
        String newClassName = r.getNewClassId() != null
                ? academicsDataProvider.findClass(r.getNewClassId()).map(gn.lacaeducation.academics.ClassPublicInfo::label).orElse(null)
                : null;
        return new PendingRefund(r.getId(), r.getStudentId(), studentName, studentNumber,
                oldClassName, newClassName, r.getAmount(), r.getReason(), r.getCreatedAt());
    }
}
