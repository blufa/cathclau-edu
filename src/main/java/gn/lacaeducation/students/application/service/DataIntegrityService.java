package gn.lacaeducation.students.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vérification/correction de la cohérence des données d'un tenant (§retour utilisateur — suppressions
 * manuelles en base, sans passer par {@link StudentPurgeService}, laissant des lignes orphelines dans
 * des tables sans contrainte de clé étrangère vers {@code students(id)} — voir la javadoc de
 * {@code StudentPurgeService} pour le même constat) : réservé SUPER_ADMIN, requêtes SQL natives pour
 * la même raison (pas de FK, pas de flux métier à respecter, outil d'administration ponctuel).
 *
 * <p>Catégories d'écarts détectées, chacune corrigible UNE PAR UNE (jamais en bloc) :</p>
 * <ul>
 *   <li>Effectif de classe ({@code school_classes.headcount}, valeur mise en cache, jamais recalculée
 *       à la lecture — voir {@code academics.EnrollmentEventListener#recomputeHeadcount}) différent
 *       du nombre réel d'inscriptions VALIDATED.</li>
 *   <li>Factures ({@code invoices.student_id}) référençant un élève qui n'existe plus.</li>
 *   <li>Paiements ({@code payments.student_id}) référençant un élève qui n'existe plus — distinct des
 *       factures orphelines : un paiement peut survivre à la suppression de sa facture.</li>
 *   <li>{@link #ORPHAN_TABLES} : les mêmes ~19 tables que {@link StudentPurgeService} (bulletins,
 *       notes, présences, documents, etc.), groupées par (table, ancien student_id) — une suppression
 *       manuelle en base laisse des résidus dans n'importe laquelle, pas seulement factures/paiements.
 *       {@code student_documents} n'a ni {@code tenant_id} ni aucune autre colonne pour retrouver
 *       l'école une fois l'élève supprimé : scannée hors scope tenant (visible quelle que soit l'école
 *       impersonnée), signalée à part dans {@link ScanResult#orphanedStudentDocuments()}.</li>
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
    /** `table` est toujours une clé de {@link #ORPHAN_TABLES} (jamais une valeur libre venue du
     * client) — {@link #deleteOrphanedRows} s'en sert pour retrouver la requête à exécuter. */
    public record OrphanRowGroup(String table, Long studentId, long rowCount) {}
    public record ScanResult(List<HeadcountMismatch> headcountMismatches,
                              List<OrphanedInvoice> orphanedInvoices,
                              List<OrphanedPayment> orphanedPayments,
                              List<OrphanRowGroup> orphanedOtherTables,
                              List<OrphanRowGroup> orphanedStudentDocuments) {}

    /** Une entrée par table nettoyée par {@link StudentPurgeService} (hors students/invoices/payments,
     * déjà couvertes par leurs propres catégories) : requête de scan groupée par student_id (bornée au
     * tenant quand une colonne/jointure le permet) + requête de suppression pour un student_id donné.
     * `report_cards` entraîne la suppression de ses `report_card_lines` avant lui-même, même ordre que
     * {@code StudentPurgeService.purge}. */
    private record TableSpec(String table, String scanSql, String deleteSql) {}

    private static final List<TableSpec> ORPHAN_TABLES = List.of(
            new TableSpec("report_cards",
                    "SELECT r.student_id, COUNT(*) FROM report_cards r "
                            + "WHERE r.class_id IN (SELECT id FROM school_classes WHERE tenant_id = :tenantId) "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = r.student_id) GROUP BY r.student_id",
                    "DELETE FROM report_card_lines WHERE report_card_id IN (SELECT id FROM report_cards WHERE student_id = :id); "
                            + "DELETE FROM report_cards WHERE student_id = :id"),
            new TableSpec("exemptions",
                    "SELECT student_id, COUNT(*) FROM exemptions WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = exemptions.student_id) GROUP BY student_id",
                    "DELETE FROM exemptions WHERE student_id = :id"),
            new TableSpec("grades",
                    "SELECT g.student_id, COUNT(*) FROM grades g "
                            + "WHERE g.assessment_id IN (SELECT id FROM assessments WHERE tenant_id = :tenantId) "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = g.student_id) GROUP BY g.student_id",
                    "DELETE FROM grades WHERE student_id = :id"),
            new TableSpec("grade_history",
                    "SELECT g.student_id, COUNT(*) FROM grade_history g "
                            + "WHERE g.assessment_id IN (SELECT id FROM assessments WHERE tenant_id = :tenantId) "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = g.student_id) GROUP BY g.student_id",
                    "DELETE FROM grade_history WHERE student_id = :id"),
            new TableSpec("assignment_submissions",
                    "SELECT student_id, COUNT(*) FROM assignment_submissions WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = assignment_submissions.student_id) GROUP BY student_id",
                    "DELETE FROM assignment_submissions WHERE student_id = :id"),
            new TableSpec("discipline_incidents",
                    "SELECT student_id, COUNT(*) FROM discipline_incidents WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = discipline_incidents.student_id) GROUP BY student_id",
                    "DELETE FROM discipline_incidents WHERE student_id = :id"),
            new TableSpec("transport_subscriptions",
                    "SELECT student_id, COUNT(*) FROM transport_subscriptions WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = transport_subscriptions.student_id) GROUP BY student_id",
                    "DELETE FROM transport_subscriptions WHERE student_id = :id"),
            new TableSpec("health_records",
                    "SELECT student_id, COUNT(*) FROM health_records WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = health_records.student_id) GROUP BY student_id",
                    "DELETE FROM health_records WHERE student_id = :id"),
            new TableSpec("infirmary_visits",
                    "SELECT student_id, COUNT(*) FROM infirmary_visits WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = infirmary_visits.student_id) GROUP BY student_id",
                    "DELETE FROM infirmary_visits WHERE student_id = :id"),
            new TableSpec("announcements",
                    "SELECT student_id, COUNT(*) FROM announcements WHERE tenant_id = :tenantId AND student_id IS NOT NULL "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = announcements.student_id) GROUP BY student_id",
                    "DELETE FROM announcements WHERE student_id = :id"),
            new TableSpec("scheduled_broadcasts",
                    "SELECT student_id, COUNT(*) FROM scheduled_broadcasts WHERE tenant_id = :tenantId AND student_id IS NOT NULL "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = scheduled_broadcasts.student_id) GROUP BY student_id",
                    "DELETE FROM scheduled_broadcasts WHERE student_id = :id"),
            new TableSpec("student_checkins",
                    "SELECT student_id, COUNT(*) FROM student_checkins WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = student_checkins.student_id) GROUP BY student_id",
                    "DELETE FROM student_checkins WHERE student_id = :id"),
            new TableSpec("child_pickups",
                    "SELECT student_id, COUNT(*) FROM child_pickups WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = child_pickups.student_id) GROUP BY student_id",
                    "DELETE FROM child_pickups WHERE student_id = :id"),
            new TableSpec("course_purchases",
                    "SELECT student_id, COUNT(*) FROM course_purchases WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = course_purchases.student_id) GROUP BY student_id",
                    "DELETE FROM course_purchases WHERE student_id = :id"),
            new TableSpec("student_card",
                    "SELECT student_id, COUNT(*) FROM student_card WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = student_card.student_id) GROUP BY student_id",
                    "DELETE FROM student_card WHERE student_id = :id"),
            new TableSpec("student_attendances",
                    "SELECT student_id, COUNT(*) FROM student_attendances WHERE tenant_id = :tenantId "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = student_attendances.student_id) GROUP BY student_id",
                    "DELETE FROM student_attendances WHERE student_id = :id"),
            new TableSpec("student_parents",
                    "SELECT sp.student_id, COUNT(*) FROM student_parents sp "
                            + "WHERE sp.parent_id IN (SELECT id FROM users WHERE tenant_id = :tenantId) "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = sp.student_id) GROUP BY sp.student_id",
                    "DELETE FROM student_parents WHERE student_id = :id"),
            new TableSpec("enrollments",
                    "SELECT e.student_id, COUNT(*) FROM enrollments e "
                            + "WHERE e.class_id IN (SELECT id FROM school_classes WHERE tenant_id = :tenantId) "
                            + "AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = e.student_id) GROUP BY e.student_id",
                    "DELETE FROM enrollments WHERE student_id = :id")
    );

    /** `student_documents` n'a ni tenant_id ni colonne de repli (classe/année/parent) — scan
     * volontairement hors scope tenant, voir javadoc de la classe. */
    private static final TableSpec STUDENT_DOCUMENTS = new TableSpec("student_documents",
            "SELECT student_id, COUNT(*) FROM student_documents "
                    + "WHERE NOT EXISTS (SELECT 1 FROM students s WHERE s.id = student_documents.student_id) GROUP BY student_id",
            "DELETE FROM student_documents WHERE student_id = :id");

    private static final Map<String, TableSpec> ORPHAN_TABLES_BY_NAME;
    static {
        Map<String, TableSpec> byName = new java.util.HashMap<>();
        ORPHAN_TABLES.forEach(t -> byName.put(t.table(), t));
        byName.put(STUDENT_DOCUMENTS.table(), STUDENT_DOCUMENTS);
        ORPHAN_TABLES_BY_NAME = Map.copyOf(byName);
    }

    @Transactional(readOnly = true)
    public ScanResult scan(Long tenantId) {
        List<OrphanRowGroup> otherTables = new ArrayList<>();
        for (TableSpec spec : ORPHAN_TABLES) {
            otherTables.addAll(scanGroups(spec, tenantId));
        }
        List<OrphanRowGroup> studentDocuments = scanGroups(STUDENT_DOCUMENTS, null);
        return new ScanResult(scanHeadcount(tenantId), scanOrphanedInvoices(tenantId), scanOrphanedPayments(tenantId),
                otherTables, studentDocuments);
    }

    @SuppressWarnings("unchecked")
    private List<OrphanRowGroup> scanGroups(TableSpec spec, Long tenantId) {
        var query = em.createNativeQuery(spec.scanSql());
        if (tenantId != null) query.setParameter("tenantId", tenantId);
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(r -> new OrphanRowGroup(spec.table(), ((Number) r[0]).longValue(), ((Number) r[1]).longValue()))
                .toList();
    }

    /** Supprime toutes les lignes orphelines d'une table pour un ancien élève — `table` doit être une
     * clé connue de {@link #ORPHAN_TABLES_BY_NAME} (jamais interpolée depuis une valeur libre). */
    @Transactional
    public void deleteOrphanedRows(String table, Long studentId) {
        TableSpec spec = ORPHAN_TABLES_BY_NAME.get(table);
        if (spec == null) {
            throw new IllegalArgumentException("Table inconnue pour la suppression de lignes orphelines : " + table);
        }
        for (String statement : spec.deleteSql().split(";\\s*")) {
            em.createNativeQuery(statement).setParameter("id", studentId).executeUpdate();
        }
        log.info("Lignes orphelines supprimées table={} student_id={}", table, studentId);
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
