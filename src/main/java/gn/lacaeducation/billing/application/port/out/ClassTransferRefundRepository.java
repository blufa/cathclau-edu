package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.ClassTransferRefund;

import java.util.List;
import java.util.Optional;

public interface ClassTransferRefundRepository {
    ClassTransferRefund save(ClassTransferRefund refund);
    Optional<ClassTransferRefund> findById(Long id);
    List<ClassTransferRefund> findByStudentId(Long studentId);
}
