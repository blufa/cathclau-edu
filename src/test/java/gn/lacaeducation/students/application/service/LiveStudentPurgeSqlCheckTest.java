package gn.lacaeducation.students.application.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Vérifie que chaque instruction SQL native de {@link StudentPurgeService} est syntaxiquement
 * valide contre le VRAI schéma de la base locale de développement (table/colonne existantes, types
 * compatibles) — via {@code EXPLAIN}, qui planifie la requête SANS JAMAIS L'EXÉCUTER (aucune ligne
 * lue ni modifiée, contrairement à {@code EXPLAIN ANALYZE}). Connexion JDBC directe (pas de contexte
 * Spring) : plus simple, et évite tout risque d'exécuter réellement les instructions via le service.
 *
 * <p>Comme {@code LiveTenant4NovemberExpenseTest} : connexion à la vraie base locale `ecole_local`,
 * absente sur tout runner CI — {@code @Disabled} en continu, à réactiver ponctuellement à la main
 * (retirer l'annotation) pour revérifier après une modification de {@link StudentPurgeService}.</p>
 */
@Disabled("Test manuel contre la vraie base locale ecole_local — jamais en CI")
class LiveStudentPurgeSqlCheckTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/ecole_local";
    private static final String USER = "blufa";
    private static final String PASSWORD = "blufa";

    // Une seule instruction par ligne, copiée EXACTEMENT de StudentPurgeService — :id remplacé par un
    // id bidon (999999999, aucune ligne réelle ne peut matcher) par simple prudence, bien qu'EXPLAIN
    // seul (sans ANALYZE) ne touche de toute façon jamais aux données.
    private static final String[] STATEMENTS = {
            "SELECT COUNT(*) FROM students WHERE id = 999999999",
            "SELECT id FROM payments WHERE student_id <> 999999999 AND student_ids LIKE '%,999999999,%'",
            "SELECT class_id, academic_year_id FROM enrollments WHERE student_id = 999999999",
            "DELETE FROM receipts WHERE payment_id IN (SELECT id FROM payments WHERE student_id = 999999999)",
            "DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE student_id = 999999999)",
            "DELETE FROM payments WHERE student_id = 999999999",
            "DELETE FROM report_card_lines WHERE report_card_id IN (SELECT id FROM report_cards WHERE student_id = 999999999)",
            "DELETE FROM report_cards WHERE student_id = 999999999",
            "DELETE FROM invoices WHERE student_id = 999999999",
            "DELETE FROM exemptions WHERE student_id = 999999999",
            "DELETE FROM grades WHERE student_id = 999999999",
            "DELETE FROM grade_history WHERE student_id = 999999999",
            "DELETE FROM assignment_submissions WHERE student_id = 999999999",
            "DELETE FROM discipline_incidents WHERE student_id = 999999999",
            "DELETE FROM transport_subscriptions WHERE student_id = 999999999",
            "DELETE FROM health_records WHERE student_id = 999999999",
            "DELETE FROM infirmary_visits WHERE student_id = 999999999",
            "DELETE FROM announcements WHERE student_id = 999999999",
            "DELETE FROM scheduled_broadcasts WHERE student_id = 999999999",
            "DELETE FROM student_checkins WHERE student_id = 999999999",
            "DELETE FROM child_pickups WHERE student_id = 999999999",
            "DELETE FROM course_purchases WHERE student_id = 999999999",
            "DELETE FROM student_card WHERE student_id = 999999999",
            "DELETE FROM student_attendances WHERE student_id = 999999999",
            "DELETE FROM student_documents WHERE student_id = 999999999",
            "DELETE FROM student_parents WHERE student_id = 999999999",
            "DELETE FROM enrollments WHERE student_id = 999999999",
            "UPDATE school_classes SET class_representative_student_id = NULL WHERE class_representative_student_id = 999999999",
            "DELETE FROM students WHERE id = 999999999",
    };

    @Test
    void everyNativeStatement_isSyntacticallyValidAgainstTheRealSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // filet de sécurité supplémentaire : rollback systématique à la fin
            try (Statement st = conn.createStatement()) {
                for (String sql : STATEMENTS) {
                    assertThatCode(() -> st.execute("EXPLAIN " + sql))
                            .as("EXPLAIN " + sql)
                            .doesNotThrowAnyException();
                }
            } finally {
                conn.rollback();
            }
        }
    }
}
