package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.application.event.EnrollmentCancelledEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suppression PHYSIQUE et DÉFINITIVE d'un élève et de TOUTES les lignes qui le référencent dans la
 * base (§retour fondateurs — SUPER_ADMIN uniquement) : contrairement à {@code StudentUseCase#delete}
 * (archivage réversible, conserve tout l'historique), cette opération ne laisse plus aucune trace —
 * irréversible, réservée au nettoyage d'un élève créé par erreur (doublon, test, mauvaise
 * manipulation) dont on veut que le "nombre de factures"/les statistiques redeviennent exactement
 * cohérents, jamais à un usage courant.
 *
 * <p><b>Pourquoi des requêtes SQL natives plutôt que les ports habituels de chaque module</b> :
 * aucune contrainte de clé étrangère ne lie ces ~20 tables à {@code students(id)} dans tout le schéma
 * (vérifié colonne par colonne) — Postgres ne bloquera donc jamais cette suppression ni ne guidera
 * vers les tables oubliées, contrairement à ce qu'on pourrait attendre. Respecter la séparation des
 * modules aurait exigé un événement consommé par chacun des ~10 modules concernés pour une opération
 * qui n'est PAS un flux métier mais un outil d'administration ponctuel — le gain de pureté
 * architecturale ne justifie pas la complexité ici.</p>
 *
 * <p><b>Cas particulier des paiements groupés (fratrie)</b> : {@code payments.student_id} porte
 * l'élève "principal" d'un paiement, mais {@code payments.student_ids} (CSV) peut aussi couvrir CET
 * élève comme simple participant d'un paiement partagé avec un frère/sœur ({@code group_lines}, en
 * JSON, est toujours renseigné en même temps que {@code student_ids} par {@code
 * PaymentService#initiateGroup} — vérifier {@code student_ids} suffit, pas besoin de parser le JSON).
 * Supprimer tout le paiement corromprait alors les données du frère/sœur — ces paiements-là sont
 * seulement SIGNALÉS (jamais touchés), à régulariser à la main.</p>
 */
@Slf4j
@Service
public class StudentPurgeService {

    @PersistenceContext
    private EntityManager em;
    // Injection par champ (pas @RequiredArgsConstructor) : pour rester cohérent avec `em` ci-dessus,
    // qui n'est injecté que par le conteneur (@PersistenceContext) — un constructeur explicite
    // casserait l'injection par champ de `em` dans les tests (Mockito @InjectMocks privilégierait
    // alors l'injection par constructeur, laissant `em` à null).
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public record PurgeResult(Long studentId, Map<String, Integer> deletedByTable, List<String> warnings) {}

    @Transactional
    public PurgeResult purge(Long studentId) {
        int exists = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM students WHERE id = :id")
                .setParameter("id", studentId).getSingleResult()).intValue();
        if (exists == 0) {
            throw new ResourceNotFoundException("Student", studentId);
        }

        Map<String, Integer> deleted = new LinkedHashMap<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();

        // Signale (sans y toucher) les paiements groupés où cet élève n'est qu'un participant parmi
        // d'autres (fratrie) — supprimer toute la ligne couperait les données d'un autre élève.
        // Format CSV entouré de virgules (ex. ",12,45,"), voir V124__payment_group_students.sql.
        List<?> sharedPaymentIds = em.createNativeQuery(
                "SELECT id FROM payments WHERE student_id <> :id AND student_ids LIKE :pattern")
                .setParameter("id", studentId)
                .setParameter("pattern", "%," + studentId + ",%")
                .getResultList();
        if (!sharedPaymentIds.isEmpty()) {
            warnings.add("Paiement(s) groupé(s) (fratrie) où l'élève n'est qu'un participant parmi "
                    + "d'autres, NON supprimés (risquerait de corrompre les données d'un frère/sœur) : "
                    + "ids=" + sharedPaymentIds + " — à régulariser manuellement si besoin.");
        }

        // Nettoie d'abord les tables qui référencent un payment_id/report_card_id de cet élève (pas de
        // FK, mais ordre plus lisible — purement cosmétique, aucune contrainte ne l'impose).
        exec(deleted, "receipts",
                "DELETE FROM receipts WHERE payment_id IN (SELECT id FROM payments WHERE student_id = :id)", studentId);
        exec(deleted, "payment_transactions",
                "DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE student_id = :id)", studentId);
        exec(deleted, "payments", "DELETE FROM payments WHERE student_id = :id", studentId);

        exec(deleted, "report_card_lines",
                "DELETE FROM report_card_lines WHERE report_card_id IN (SELECT id FROM report_cards WHERE student_id = :id)", studentId);
        exec(deleted, "report_cards", "DELETE FROM report_cards WHERE student_id = :id", studentId);

        exec(deleted, "invoices", "DELETE FROM invoices WHERE student_id = :id", studentId);
        exec(deleted, "exemptions", "DELETE FROM exemptions WHERE student_id = :id", studentId);
        exec(deleted, "grades", "DELETE FROM grades WHERE student_id = :id", studentId);
        exec(deleted, "grade_history", "DELETE FROM grade_history WHERE student_id = :id", studentId);
        exec(deleted, "assignment_submissions", "DELETE FROM assignment_submissions WHERE student_id = :id", studentId);
        exec(deleted, "discipline_incidents", "DELETE FROM discipline_incidents WHERE student_id = :id", studentId);
        exec(deleted, "transport_subscriptions", "DELETE FROM transport_subscriptions WHERE student_id = :id", studentId);
        exec(deleted, "health_records", "DELETE FROM health_records WHERE student_id = :id", studentId);
        exec(deleted, "infirmary_visits", "DELETE FROM infirmary_visits WHERE student_id = :id", studentId);
        exec(deleted, "announcements", "DELETE FROM announcements WHERE student_id = :id", studentId);
        exec(deleted, "scheduled_broadcasts", "DELETE FROM scheduled_broadcasts WHERE student_id = :id", studentId);
        exec(deleted, "student_checkins", "DELETE FROM student_checkins WHERE student_id = :id", studentId);
        exec(deleted, "child_pickups", "DELETE FROM child_pickups WHERE student_id = :id", studentId);
        exec(deleted, "course_purchases", "DELETE FROM course_purchases WHERE student_id = :id", studentId);
        exec(deleted, "student_card", "DELETE FROM student_card WHERE student_id = :id", studentId);
        exec(deleted, "student_attendances", "DELETE FROM student_attendances WHERE student_id = :id", studentId);
        exec(deleted, "student_documents", "DELETE FROM student_documents WHERE student_id = :id", studentId);
        exec(deleted, "student_parents", "DELETE FROM student_parents WHERE student_id = :id", studentId);

        // Capturé AVANT la suppression (§retour comptable : l'effectif d'une classe restait gonflé
        // après une purge) — l'effectif de classe n'est pas recompté à la lecture mais mis en cache
        // sur school_classes.headcount, recalculé uniquement en réaction à un événement (voir
        // academics.EnrollmentEventListener) ; la suppression SQL native ci-dessous ne publie rien
        // toute seule, contrairement au flux normal (InscriptionService#cancel).
        @SuppressWarnings("unchecked")
        List<Object[]> enrollmentClassYears = em.createNativeQuery(
                "SELECT class_id, academic_year_id FROM enrollments WHERE student_id = :id")
                .setParameter("id", studentId).getResultList();

        exec(deleted, "enrollments", "DELETE FROM enrollments WHERE student_id = :id", studentId);

        // Référence FROM une classe VERS cet élève (représentant de classe) — ne supprime pas la
        // classe, annule juste le pointeur devenu orphelin.
        int reps = em.createNativeQuery(
                "UPDATE school_classes SET class_representative_student_id = NULL WHERE class_representative_student_id = :id")
                .setParameter("id", studentId).executeUpdate();
        if (reps > 0) deleted.put("school_classes.class_representative_student_id (mis à NULL)", reps);

        exec(deleted, "students", "DELETE FROM students WHERE id = :id", studentId);

        // Même événement que InscriptionService#cancel (tenantId non utilisé par les deux
        // consommateurs actuels — recalcul d'effectif côté academics, journal d'audit) : chaque classe
        // où l'élève était inscrit retrouve immédiatement le bon effectif, sans dupliquer la logique
        // de recalcul ici.
        for (Object[] row : enrollmentClassYears) {
            Long classId = row[0] == null ? null : ((Number) row[0]).longValue();
            Long academicYearId = row[1] == null ? null : ((Number) row[1]).longValue();
            if (classId != null) {
                eventPublisher.publishEvent(new EnrollmentCancelledEvent(null, studentId, classId, academicYearId));
            }
        }

        log.warn("PURGE DÉFINITIVE de l'élève id={} effectuée — {} table(s) nettoyée(s), {} avertissement(s) : {}",
                studentId, deleted.size(), warnings.size(), warnings);
        return new PurgeResult(studentId, deleted, warnings);
    }

    private void exec(Map<String, Integer> deleted, String table, String sql, Long studentId) {
        int count = em.createNativeQuery(sql).setParameter("id", studentId).executeUpdate();
        if (count > 0) deleted.put(table, count);
    }
}
