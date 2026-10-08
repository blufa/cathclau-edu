package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.application.event.EnrollmentCancelledEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
    @Mock ApplicationEventPublisher eventPublisher;
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
        Query sharedPaymentsQuery = org.mockito.Mockito.mock(Query.class);
        when(em.createNativeQuery(eq("SELECT id FROM payments WHERE student_id <> :id AND student_ids LIKE :pattern")))
                .thenReturn(sharedPaymentsQuery);
        when(sharedPaymentsQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(sharedPaymentsQuery);
        when(sharedPaymentsQuery.getResultList()).thenReturn(List.of(42L));

        StudentPurgeService.PurgeResult result = service.purge(7L);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("42").contains("fratrie");
    }

    /**
     * Retour comptable réel : l'effectif de la classe restait gonflé après une purge, parce que
     * school_classes.headcount est un compteur mis en cache (recalculé seulement en réaction à un
     * événement, voir academics.EnrollmentEventListener) — une suppression SQL native directe ne
     * déclenche rien toute seule, contrairement au flux normal (InscriptionService#cancel).
     */
    @Test
    void purge_studentHadEnrollments_publishesCancelledEventPerEnrollmentForHeadcountRecompute() {
        when(em.createNativeQuery(eq("SELECT COUNT(*) FROM students WHERE id = :id"))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(em.createNativeQuery(eq("SELECT class_id, academic_year_id FROM enrollments WHERE student_id = :id")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{5L, 20L}, new Object[]{6L, 20L}));

        service.purge(7L);

        verify(eventPublisher).publishEvent(new EnrollmentCancelledEvent(null, 7L, 5L, 20L));
        verify(eventPublisher).publishEvent(new EnrollmentCancelledEvent(null, 7L, 6L, 20L));
    }
}
