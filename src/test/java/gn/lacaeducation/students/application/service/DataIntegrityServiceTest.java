package gn.lacaeducation.students.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code EntityManager} mocké (même limite que {@code StudentPurgeServiceTest} : pas de base réelle
 * dans les tests de ce projet) — valide le CONTRÔLE (filtrage des écarts, requêtes correctes
 * déclenchées), pas le texte SQL natif lui-même. Vérifié manuellement contre le schéma avant usage.
 */
@ExtendWith(MockitoExtension.class)
class DataIntegrityServiceTest {

    @Mock EntityManager em;
    @Mock Query query;
    @InjectMocks DataIntegrityService service;

    @BeforeEach
    void setUp() {
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        lenient().when(query.executeUpdate()).thenReturn(0);
        lenient().when(query.getResultList()).thenReturn(List.of());
    }

    private static final String HEADCOUNT_SQL =
            "SELECT c.id, c.label, c.headcount, "
                    + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id AND e.status = 'VALIDATED') "
                    + "FROM school_classes c WHERE c.tenant_id = :tenantId";
    private static final String ORPHANED_INVOICES_SQL =
            "SELECT i.id, i.student_id, i.reference, i.remaining_balance FROM invoices i "
                    + "WHERE i.tenant_id = :tenantId AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = i.student_id)";
    private static final String ORPHANED_PAYMENTS_SQL =
            "SELECT p.id, p.student_id, p.reference, p.amount FROM payments p "
                    + "WHERE p.tenant_id = :tenantId AND NOT EXISTS (SELECT 1 FROM students s WHERE s.id = p.student_id)";

    @Test
    void scan_headcountMatchesActual_notReportedAsMismatch() {
        Query headcountQuery = mock(Query.class);
        when(em.createNativeQuery(eq(HEADCOUNT_SQL))).thenReturn(headcountQuery);
        when(headcountQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(headcountQuery);
        when(headcountQuery.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{1L, "6ème A", 25, 25L}));

        DataIntegrityService.ScanResult result = service.scan(10L);

        assertThat(result.headcountMismatches()).isEmpty();
    }

    @Test
    void scan_headcountDiffersFromActualEnrollments_reportedAsMismatch() {
        Query headcountQuery = mock(Query.class);
        when(em.createNativeQuery(eq(HEADCOUNT_SQL))).thenReturn(headcountQuery);
        when(headcountQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(headcountQuery);
        // Retour comptable réel : effectif en cache resté à 25 après une suppression manuelle en
        // base, alors que seuls 23 élèves ont une inscription VALIDATED.
        when(headcountQuery.getResultList()).thenReturn(java.util.Collections.singletonList(new Object[]{1L, "6ème A", 25, 23L}));

        DataIntegrityService.ScanResult result = service.scan(10L);

        assertThat(result.headcountMismatches()).hasSize(1);
        DataIntegrityService.HeadcountMismatch m = result.headcountMismatches().get(0);
        assertThat(m.classId()).isEqualTo(1L);
        assertThat(m.cachedHeadcount()).isEqualTo(25);
        assertThat(m.actualValidatedEnrollments()).isEqualTo(23L);
    }

    @Test
    void scan_orphanedInvoice_reported() {
        Query invoicesQuery = mock(Query.class);
        when(em.createNativeQuery(eq(ORPHANED_INVOICES_SQL))).thenReturn(invoicesQuery);
        when(invoicesQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(invoicesQuery);
        when(invoicesQuery.getResultList()).thenReturn(
                java.util.Collections.singletonList(new Object[]{99L, 7L, "EC-INS-2026-X", new BigDecimal("80000.00")}));

        DataIntegrityService.ScanResult result = service.scan(10L);

        assertThat(result.orphanedInvoices()).hasSize(1);
        assertThat(result.orphanedInvoices().get(0).invoiceId()).isEqualTo(99L);
        assertThat(result.orphanedInvoices().get(0).studentId()).isEqualTo(7L);
    }

    @Test
    void scan_orphanedPayment_reported() {
        Query paymentsQuery = mock(Query.class);
        when(em.createNativeQuery(eq(ORPHANED_PAYMENTS_SQL))).thenReturn(paymentsQuery);
        when(paymentsQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(paymentsQuery);
        when(paymentsQuery.getResultList()).thenReturn(
                java.util.Collections.singletonList(new Object[]{55L, 7L, "PAY-GRP-7-X", new BigDecimal("60000.00")}));

        DataIntegrityService.ScanResult result = service.scan(10L);

        assertThat(result.orphanedPayments()).hasSize(1);
        assertThat(result.orphanedPayments().get(0).paymentId()).isEqualTo(55L);
    }

    @Test
    void fixHeadcount_recomputesFromValidatedEnrollments_andSaves() {
        Query countQuery = mock(Query.class);
        when(em.createNativeQuery(eq("SELECT COUNT(*) FROM enrollments WHERE class_id = :classId AND status = 'VALIDATED'")))
                .thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(23L);

        int result = service.fixHeadcount(1L);

        assertThat(result).isEqualTo(23);
        verify(query).executeUpdate(); // l'UPDATE school_classes
    }

    @Test
    void deleteOrphanedInvoice_deletesReceiptsTransactionsPaymentsAndInvoice() {
        service.deleteOrphanedInvoice(99L);

        verify(em).createNativeQuery(eq("DELETE FROM receipts WHERE payment_id IN (SELECT id FROM payments WHERE invoice_id = :id)"));
        verify(em).createNativeQuery(eq("DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE invoice_id = :id)"));
        verify(em).createNativeQuery(eq("DELETE FROM payments WHERE invoice_id = :id"));
        verify(em).createNativeQuery(eq("DELETE FROM invoices WHERE id = :id"));
    }

    @Test
    void deleteOrphanedPayment_deletesReceiptsTransactionsAndPayment() {
        service.deleteOrphanedPayment(55L);

        verify(em).createNativeQuery(eq("DELETE FROM receipts WHERE payment_id = :id"));
        verify(em).createNativeQuery(eq("DELETE FROM payment_transactions WHERE payment_id = :id"));
        verify(em).createNativeQuery(eq("DELETE FROM payments WHERE id = :id"));
    }
}
