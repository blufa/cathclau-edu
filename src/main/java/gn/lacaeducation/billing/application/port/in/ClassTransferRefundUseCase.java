package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.ClassTransferRefund;

import java.math.BigDecimal;
import java.util.List;

public interface ClassTransferRefundUseCase {
    ClassTransferRefund record(Long tenantId, Long studentId, Long oldClassId, Long newClassId,
                               BigDecimal amount, String reason);
    /** Tous les surplus de CET élève, remboursés ou non (le plus récent en premier). */
    List<ClassTransferRefund> findByStudentId(Long studentId);
    /** Uniquement ceux pas encore remboursés — c'est cette liste qui doit apparaître sur un reçu. */
    List<ClassTransferRefund> findPendingByStudentId(Long studentId);
    ClassTransferRefund markRefunded(Long id);

    /** Surplus en attente, tenant courant, noms déjà résolus (élève/classes) — pour un écran de
     * régularisation (§retour comptable : "ajouter un écran pour voir/régulariser ces surplus"), aucun
     * identifiant brut à afficher. */
    record PendingRefund(Long id, Long studentId, String studentName, String studentNumber,
                         String oldClassName, String newClassName, BigDecimal amount, String reason,
                         java.time.LocalDateTime createdAt) {}

    List<PendingRefund> findAllPending();
}
