package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.ClassTransferRefund;

import java.util.List;
import java.util.Optional;

public interface ClassTransferRefundRepository {
    ClassTransferRefund save(ClassTransferRefund refund);
    Optional<ClassTransferRefund> findById(Long id);
    List<ClassTransferRefund> findByStudentId(Long studentId);
    /** Tenant du contexte courant (filtre Hibernate déjà actif) — le plus ancien d'abord (§retour
     * comptable : régulariser les surplus en attente depuis le plus longtemps). */
    List<ClassTransferRefund> findPending();
}
