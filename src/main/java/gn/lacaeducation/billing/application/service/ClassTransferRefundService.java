package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ClassTransferRefundUseCase;
import gn.lacaeducation.billing.application.port.out.ClassTransferRefundRepository;
import gn.lacaeducation.billing.domain.ClassTransferRefund;
import gn.lacaeducation.common.ResourceNotFoundException;
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
}
