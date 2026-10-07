package gn.lacaeducation.students.application.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie, en LECTURE SEULE, qu'aucune trace du test {@link LiveStudentPurgeEndToEndTest} (dont la
 * transaction est annulée) n'est restée dans la vraie base locale — filet de sécurité supplémentaire,
 * exécuté séparément après coup. {@code @Disabled} en continu, comme les autres tests "Live*".
 */
@Disabled("Test manuel contre la vraie base locale ecole_local — jamais en CI")
class LiveStudentPurgeVerifyNoLeakTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/ecole_local";
    private static final String USER = "blufa";
    private static final String PASSWORD = "blufa";

    @Test
    void noZzzPurgeTestDataRemains() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM students WHERE first_name = 'ZZZ_PURGE_TEST'");
            rs.next();
            assertThat(rs.getLong(1)).as("aucun élève jetable ne doit avoir persisté").isZero();

            rs = st.executeQuery("SELECT COUNT(*) FROM payments WHERE reference LIKE 'ZZZ-PURGE-TEST%'");
            rs.next();
            assertThat(rs.getLong(1)).as("aucun paiement jetable ne doit avoir persisté").isZero();

            rs = st.executeQuery("SELECT COUNT(*) FROM invoices WHERE reference = 'ZZZ-PURGE-TEST-INV'");
            rs.next();
            assertThat(rs.getLong(1)).as("aucune facture jetable ne doit avoir persisté").isZero();
        }
    }
}
