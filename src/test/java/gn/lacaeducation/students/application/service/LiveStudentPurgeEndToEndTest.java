package gn.lacaeducation.students.application.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie {@link StudentPurgeService} de bout en bout contre la VRAIE base locale de développement :
 * crée un élève jetable (id improbable, préfixé "ZZZ_PURGE_TEST") avec une inscription, une facture
 * et un paiement, exécute EXACTEMENT les instructions du service (copiées, pas d'appel au service lui-
 * même — évite d'avoir à démarrer tout le contexte Spring juste pour ce test), vérifie que tout
 * disparaît, PUIS annule la transaction (jamais de COMMIT) — la vraie base locale n'est donc jamais
 * modifiée, même en cas d'échec de l'assertion.
 *
 * <p>Comme {@code LiveTenant4NovemberExpenseTest}/{@code LiveStudentPurgeSqlCheckTest} : {@code
 * @Disabled} en continu (jamais en CI), à réactiver ponctuellement à la main pour revérifier après
 * une modification de {@link StudentPurgeService}.</p>
 */
@Disabled("Test manuel contre la vraie base locale ecole_local — jamais en CI")
class LiveStudentPurgeEndToEndTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/ecole_local";
    private static final String USER = "blufa";
    private static final String PASSWORD = "blufa";
    private static final long FAKE_TENANT_ID = 999999999L;
    private static final long FAKE_SIBLING_STUDENT_ID = 999999998L;

    @Test
    void purge_realExecution_removesEverythingThenRollsBack() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            try {
                long studentId = insertStudent(conn);
                insertEnrollment(conn, studentId);
                long invoiceId = insertInvoice(conn, studentId);
                insertPayment(conn, studentId, invoiceId);
                // Paiement partagé avec un autre élève où CET élève n'est qu'un participant — jamais touché.
                long sharedPaymentId = insertSharedPayment(conn, studentId);

                assertRowCount(conn, "students", studentId, 1);
                assertRowCount(conn, "enrollments", studentId, 1);
                assertRowCount(conn, "invoices", studentId, 1);
                assertRowCount(conn, "payments", studentId, 1);

                executePurgeStatements(conn, studentId);

                assertRowCount(conn, "students", studentId, 0);
                assertRowCount(conn, "enrollments", studentId, 0);
                assertRowCount(conn, "invoices", studentId, 0);
                assertRowCount(conn, "payments", studentId, 0);
                // Le paiement partagé doit TOUJOURS exister — jamais supprimé.
                try (Statement st = conn.createStatement()) {
                    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM payments WHERE id = " + sharedPaymentId);
                    rs.next();
                    assertThat(rs.getLong(1)).as("paiement partagé, ne doit jamais être supprimé").isEqualTo(1);
                }
            } finally {
                conn.rollback(); // la vraie base locale n'est JAMAIS modifiée, même si une assertion échoue au-dessus.
            }
        }
    }

    private long insertStudent(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                    "INSERT INTO students (tenant_id, first_name, last_name, student_number) "
                            + "VALUES (" + FAKE_TENANT_ID + ", 'ZZZ_PURGE_TEST', 'DELETE_ME', 'ZZZ-PURGE-TEST') RETURNING id");
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insertEnrollment(Connection conn, long studentId) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO enrollments (student_id, class_id, academic_year_id, type, status) "
                    + "VALUES (" + studentId + ", " + FAKE_TENANT_ID + ", " + FAKE_TENANT_ID + ", 'NEW', 'PENDING')");
        }
    }

    private long insertInvoice(Connection conn, long studentId) throws Exception {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                    "INSERT INTO invoices (tenant_id, student_id, reference, total_amount, remaining_balance, status, issue_date) "
                            + "VALUES (" + FAKE_TENANT_ID + ", " + studentId + ", 'ZZZ-PURGE-TEST-INV', 100000, 100000, 'UNPAID', CURRENT_DATE) "
                            + "RETURNING id");
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insertPayment(Connection conn, long studentId, long invoiceId) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO payments (tenant_id, invoice_id, student_id, reference, amount, channel, type, status) "
                    + "VALUES (" + FAKE_TENANT_ID + ", " + invoiceId + ", " + studentId + ", 'ZZZ-PURGE-TEST-PAY', 100000, "
                    + "'ORANGE_MONEY', 'COMPLETE', 'SUCCEEDED')");
        }
    }

    private long insertSharedPayment(Connection conn, long studentId) throws Exception {
        try (Statement st = conn.createStatement()) {
            // student_id = le "frère/sœur" (principal du paiement), student_ids contient les DEUX —
            // exactement le format CSV entouré de virgules de V124__payment_group_students.sql.
            ResultSet rs = st.executeQuery(
                    "INSERT INTO payments (tenant_id, invoice_id, student_id, student_ids, reference, amount, channel, type, status) "
                            + "VALUES (" + FAKE_TENANT_ID + ", " + FAKE_TENANT_ID + ", " + FAKE_SIBLING_STUDENT_ID + ", "
                            + "',%s,%s,', 'ZZZ-PURGE-TEST-SHARED-PAY', 150000, 'WAVE', 'COMPLETE', 'SUCCEEDED') RETURNING id"
                                    .formatted(studentId, FAKE_SIBLING_STUDENT_ID));
            rs.next();
            return rs.getLong(1);
        }
    }

    private void assertRowCount(Connection conn, String table, long studentId, int expected) throws Exception {
        String column = table.equals("students") ? "id" : "student_id";
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = " + studentId);
            rs.next();
            assertThat(rs.getLong(1)).as(table).isEqualTo(expected);
        }
    }

    /** Copie EXACTE des instructions de {@link StudentPurgeService#purge}, avec studentId substitué
     * directement (test uniquement — le service lui-même utilise toujours des paramètres liés). */
    private void executePurgeStatements(Connection conn, long id) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM receipts WHERE payment_id IN (SELECT id FROM payments WHERE student_id = " + id + ")");
            st.execute("DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE student_id = " + id + ")");
            st.execute("DELETE FROM payments WHERE student_id = " + id);
            st.execute("DELETE FROM report_card_lines WHERE report_card_id IN (SELECT id FROM report_cards WHERE student_id = " + id + ")");
            st.execute("DELETE FROM report_cards WHERE student_id = " + id);
            st.execute("DELETE FROM invoices WHERE student_id = " + id);
            st.execute("DELETE FROM exemptions WHERE student_id = " + id);
            st.execute("DELETE FROM grades WHERE student_id = " + id);
            st.execute("DELETE FROM grade_history WHERE student_id = " + id);
            st.execute("DELETE FROM assignment_submissions WHERE student_id = " + id);
            st.execute("DELETE FROM discipline_incidents WHERE student_id = " + id);
            st.execute("DELETE FROM transport_subscriptions WHERE student_id = " + id);
            st.execute("DELETE FROM health_records WHERE student_id = " + id);
            st.execute("DELETE FROM infirmary_visits WHERE student_id = " + id);
            st.execute("DELETE FROM announcements WHERE student_id = " + id);
            st.execute("DELETE FROM scheduled_broadcasts WHERE student_id = " + id);
            st.execute("DELETE FROM student_checkins WHERE student_id = " + id);
            st.execute("DELETE FROM child_pickups WHERE student_id = " + id);
            st.execute("DELETE FROM course_purchases WHERE student_id = " + id);
            st.execute("DELETE FROM student_card WHERE student_id = " + id);
            st.execute("DELETE FROM student_attendances WHERE student_id = " + id);
            st.execute("DELETE FROM student_documents WHERE student_id = " + id);
            st.execute("DELETE FROM student_parents WHERE student_id = " + id);
            st.execute("DELETE FROM enrollments WHERE student_id = " + id);
            st.execute("UPDATE school_classes SET class_representative_student_id = NULL WHERE class_representative_student_id = " + id);
            st.execute("DELETE FROM students WHERE id = " + id);
        }
    }
}
