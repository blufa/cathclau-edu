package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Teste uniquement le CONTRÔLE (garde "élève introuvable", détection des paiements groupés
 * partagés) — {@code EntityManager} étant mocké, ces tests ne valident PAS le texte SQL natif
 * lui-même (aucune infrastructure de test avec une vraie base dans ce projet, voir
 * {@code LacaeducationApplicationTests}, @Disabled). Les noms de table/colonne ont été vérifiés
 * manuellement contre chaque migration (voir Javadoc de la classe) ; à tester en conditions réelles
 * sur une base de développement avant tout usage en production, étant donné le caractère
 * irréversible de cette opération.
 */
@ExtendWith(MockitoExtension.class)
class StudentPurgeServiceTest {

    @Mock EntityManager em;
    @Mock Query query;
    @InjectMocks StudentPurgeService service;

    @BeforeEach
    void setUp() {
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        lenient().when(query.executeUpdate()).thenReturn(0);
        lenient().when(query.getResultList()).thenReturn(List.of());
    }

    @Test
    void purge_unknownStudent_throwsNotFound() {
        when(em.createNativeQuery(eq("SELECT COUNT(*) FROM students WHERE id = :id"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        assertThatThrownBy(() -> service.purge(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purge_existingStudent_deletesAcrossTablesAndReturnsSummary() {
        when(em.createNativeQuery(eq("SELECT COUNT(*) FROM students WHERE id = :id"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(query.executeUpdate()).thenReturn(1); // every DELETE/UPDATE reports exactly 1 row removed

        StudentPurgeService.PurgeResult result = service.purge(7L);

        assertThat(result.studentId()).isEqualTo(7L);
        // Au moins les tables les plus critiques pour la cohérence mentionnée par l'utilisateur
        // (nombre de factures) doivent apparaître dans le résumé.
        assertThat(result.deletedByTable()).containsKeys("invoices", "payments", "enrollments", "students");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void purge_studentSharesAGroupedPayment_warnsWithoutDeletingIt() {
        when(em.createNativeQuery(eq("SELECT COUNT(*) FROM students WHERE id = :id"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(em.createNativeQuery(eq("SELECT id FROM payments WHERE student_id <> :id AND student_ids LIKE :pattern")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(42L));

        StudentPurgeService.PurgeResult result = service.purge(7L);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("42").contains("fratrie");
    }
}
