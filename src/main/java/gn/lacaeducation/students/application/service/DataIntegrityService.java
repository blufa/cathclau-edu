package gn.lacaeducation.students.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vérification/correction de la cohérence des données d'un tenant (§retour utilisateur — suppressions
 * manuelles en base, sans passer par {@link StudentPurgeService}, laissant des lignes orphelines dans
 * des tables sans contrainte de clé étrangère vers {@code students(id)} — voir la javadoc de
 * {@code StudentPurgeService} pour le même constat) : réservé SUPER_ADMIN, requêtes SQL natives pour
 * la même raison (pas de FK, pas de flux métier à respecter, outil d'administration ponctuel).
 *
 * <p>Trois catégories d'écarts détectés, chacune corrigible UNE PAR UNE (jamais en bloc) :</p>
 * <ul>
 *   <li>Effectif de classe ({@code school_classes.headcount}, valeur mise en cache, jamais recalculée
 *       à la lecture — voir {@code academics.EnrollmentEventListener#recomputeHeadcount}) différent
 *       du nombre réel d'inscriptions VALIDATED.</li>
 *   <li>Factures ({@code invoices.student_id}) référençant un élève qui n'existe plus.</li>
 *   <li>Paiements ({@code payments.student_id}) référençant un élève qui n'existe plus — distinct des
 *       factures orphelines : un paiement peut survivre à la suppression de sa facture.</li>
 * </ul>
 */
@Slf4j
@Service
public class DataIntegrityService {

    @PersistenceContext
    private EntityManager em;

    public record HeadcountMismatch(Long classId, String className, int cachedHeadcount, long actualValidatedEnrollments) {}
    public record OrphanedInvoice(Long invoiceId, Long studentId, String reference, BigDecimal remainingBalance) {}
    public record OrphanedPayment(Long paymentId, Long studentId, String reference, BigDecimal amount) {}
    public record ScanResult(List<HeadcountMismatch> headcountMismatches,
                              List<OrphanedInvoice> orphanedInvoices,
                              List<OrphanedPayment> orphanedPayments) {}

    @Transactional(readOnly = true)
    public ScanResult scan(Long tenantId) {
        return new ScanResult(scanHeadcount(tenantId), scanOrphanedInvoices(tenantId), scanOrphanedPayments(tenantId));
    }

    @SuppressWarnings("unchecked")
    private List<HeadcountMismatch> scanHeadcount(Long tenantId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.id, c.label, c.headcount, "
                        + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id AND e.status = 'VALIDATED') "
                        + "FROM school_classes c WHERE c.tenant_id = :tenantId")
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.stream()
                .map(r -> new HeadcountMismatch(
                        ((Number) r[0]).longValue(), (String) r[1], ((Number) r[2]).intValue(), ((Number) r[3]).longValue()))
                .filter(m -> m.cachedHeadcount() != m.actualValidatedEnrollments())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<OrphanedInvoice> scanOrphanedInvoices(Long tenantId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT i.id, i.student_id, i.reference, i.remaining_balance FROM invoices i "
                        + "WHERE i.tenant_id = :tenantId AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = i.student_id)")
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.stream()
                .map(r -> new OrphanedInvoice(((Number) r[0]).longValue(), ((Number) r[1]).longValue(),
                        (String) r[2], (BigDecimal) r[3]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<OrphanedPayment> scanOrphanedPayments(Long tenantId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT p.id, p.student_id, p.reference, p.amount FROM payments p "
                        + "WHERE p.tenant_id = :tenantId AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = p.student_id)")
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.stream()
                .map(r -> new OrphanedPayment(((Number) r[0]).longValue(), ((Number) r[1]).longValue(),
                        (String) r[2], (BigDecimal) r[3]))
                .toList();
    }

    /** Recalcule l'effectif réel d'une classe et l'enregistre — même calcul que
     * {@code academics.EnrollmentEventListener#recomputeHeadcount}, répliqué en SQL natif ici pour
     * rester un outil autonome (voir javadoc de la classe). */
    @Transactional
    public int fixHeadcount(Long classId) {
        long actual = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM enrollments WHERE class_id = :classId AND status = 'VALIDATED'")
                .setParameter("classId", classId).getSingleResult()).longValue();
        em.createNativeQuery("UPDATE school_classes SET headcount = :headcount WHERE id = :classId")
                .setParameter("headcount", (int) actual)
                .setParameter("classId", classId)
                .executeUpdate();
        log.info("Effectif de la classe id={} recalculé à {}", classId, actual);
        return (int) actual;
    }

    /** Supprime une facture orpheline (élève déjà supprimé) et les lignes qui la référencent. */
    @Transactional
    public void deleteOrphanedInvoice(Long invoiceId) {
        em.createNativeQuery("DELETE FROM receipts WHERE payment_id IN (SELECT id FROM payments WHERE invoice_id = :id)")
                .setParameter("id", invoiceId).executeUpdate();
        em.createNativeQuery("DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE invoice_id = :id)")
                .setParameter("id", invoiceId).executeUpdate();
        em.createNativeQuery("DELETE FROM payments WHERE invoice_id = :id").setParameter("id", invoiceId).executeUpdate();
        em.createNativeQuery("DELETE FROM invoices WHERE id = :id").setParameter("id", invoiceId).executeUpdate();
        log.info("Facture orpheline id={} supprimée", invoiceId);
    }

    /** Supprime un paiement orphelin (élève déjà supprimé) et les lignes qui le référencent. */
    @Transactional
    public void deleteOrphanedPayment(Long paymentId) {
        em.createNativeQuery("DELETE FROM receipts WHERE payment_id = :id").setParameter("id", paymentId).executeUpdate();
        em.createNativeQuery("DELETE FROM payment_transactions WHERE payment_id = :id").setParameter("id", paymentId).executeUpdate();
        em.createNativeQuery("DELETE FROM payments WHERE id = :id").setParameter("id", paymentId).executeUpdate();
        log.info("Paiement orphelin id={} supprimé", paymentId);
    }
}
