package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.StaffLinkType;
import gn.lacaeducation.students.domain.StudentParent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentAccessProviderAdapterTest {

    @Mock ParentRepository parentRepository;
    @Mock StudentParentRepository studentParentRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @InjectMocks ParentAccessProviderAdapter adapter;

    @Test
    void isParentOfStudent_linkedChild_returnsTrue() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));

        assertThat(adapter.isParentOfStudent(7L, 11L)).isTrue();
    }

    @Test
    void isParentOfStudent_otherChild_returnsFalse() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));

        assertThat(adapter.isParentOfStudent(7L, 99L)).isFalse();
    }

    @Test
    void isParentOfStudent_notAParent_returnsFalse() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of());

        assertThat(adapter.isParentOfStudent(7L, 11L)).isFalse();
    }

    @Test
    void isParentOfStudent_nullArgs_returnsFalse() {
        assertThat(adapter.isParentOfStudent(null, 11L)).isFalse();
    }

    @Test
    void findChildStudentIdsEnrolled_excludesOnlyCancelledOrAbandoned() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(
                List.of(Enrollment.builder().studentId(10L).status(EnrollmentStatus.VALIDATED).build()));
        when(enrollmentRepository.findByStudentId(11L)).thenReturn(
                List.of(Enrollment.builder().studentId(11L).status(EnrollmentStatus.ABANDONED).build()));

        assertThat(adapter.findChildStudentIdsEnrolled(7L)).containsExactly(10L);
    }

    @Test
    void findChildStudentIdsEnrolled_pendingEnrollment_stillCounted() {
        // Retour comptable : un personnel qui laisse l'inscription de son enfant en attente (jamais
        // validée par le secrétariat) ne doit pas pouvoir échapper à la retenue salariale pour autant.
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L));
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(
                List.of(Enrollment.builder().studentId(10L).status(EnrollmentStatus.PENDING).build()));

        assertThat(adapter.findChildStudentIdsEnrolled(7L)).containsExactly(10L);
    }

    @Test
    void findChildStudentIdsEnrolled_notAParent_returnsEmpty() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of());

        assertThat(adapter.findChildStudentIdsEnrolled(7L)).isEmpty();
    }

    @Test
    void findChildStudentIds_duplicateParentRowsSharingUserId_aggregatesAcrossAll() {
        // Cas réel : personnel de l'école lié comme parent via une fiche différente à chaque enfant
        // inscrit, toutes partageant le même userId (au lieu de réutiliser la même fiche Parent).
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(
                Parent.builder().id(3L).userId(7L).build(),
                Parent.builder().id(5L).userId(7L).build(),
                Parent.builder().id(9L).userId(7L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L));
        when(studentParentRepository.findStudentIdsByParentId(5L)).thenReturn(List.of(11L));
        when(studentParentRepository.findStudentIdsByParentId(9L)).thenReturn(List.of(12L));

        assertThat(adapter.findChildStudentIds(7L)).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void findChildStudentIdsEnrolledByStaffLink_resolvesViaDirectLink_pendingIncludedAbandonedExcluded() {
        // Personnel SANS compte de connexion (lien direct staffType/staffId au lieu de userId) :
        // régression découverte en vrai — la retenue sur salaire ne trouvait jamais ces enfants.
        when(parentRepository.findAllByLinkedStaff(StaffLinkType.TEACHER, 36L))
                .thenReturn(List.of(Parent.builder().id(59L).build()));
        when(studentParentRepository.findStudentIdsByParentId(59L)).thenReturn(List.of(137L, 138L, 139L));
        when(enrollmentRepository.findByStudentId(137L)).thenReturn(
                List.of(Enrollment.builder().studentId(137L).status(EnrollmentStatus.VALIDATED).build()));
        when(enrollmentRepository.findByStudentId(138L)).thenReturn(
                List.of(Enrollment.builder().studentId(138L).status(EnrollmentStatus.PENDING).build()));
        when(enrollmentRepository.findByStudentId(139L)).thenReturn(
                List.of(Enrollment.builder().studentId(139L).status(EnrollmentStatus.ABANDONED).build()));

        assertThat(adapter.findChildStudentIdsEnrolledByStaffLink("TEACHER", 36L)).containsExactlyInAnyOrder(137L, 138L);
    }

    @Test
    void findChildStudentIdsEnrolledByStaffLink_nullArgs_returnsEmpty() {
        assertThat(adapter.findChildStudentIdsEnrolledByStaffLink(null, 36L)).isEmpty();
        assertThat(adapter.findChildStudentIdsEnrolledByStaffLink("TEACHER", null)).isEmpty();
    }

    @Test
    void findChildStudentIdsEnrolledByStaffLink_unknownStaffType_returnsEmpty() {
        assertThat(adapter.findChildStudentIdsEnrolledByStaffLink("BOGUS", 36L)).isEmpty();
    }

    @Test
    void hasOptedOutOfStaffAutoDeduction_anyLinkedParentOptedOut_returnsTrue() {
        when(studentParentRepository.findParentsByStudentId(11L)).thenReturn(List.of(
                Parent.builder().id(3L).excludeFromAutoDeduction(false).build(),
                Parent.builder().id(4L).excludeFromAutoDeduction(true).build()));

        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(11L)).isTrue();
    }

    @Test
    void hasOptedOutOfStaffAutoDeduction_noParentOptedOut_returnsFalse() {
        when(studentParentRepository.findParentsByStudentId(11L)).thenReturn(List.of(
                Parent.builder().id(3L).excludeFromAutoDeduction(false).build()));

        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(11L)).isFalse();
    }

    @Test
    void hasOptedOutOfStaffAutoDeduction_nullStudentId_returnsFalse() {
        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(null)).isFalse();
    }

    @Test
    void hasOptedOutOfStaffAutoDeduction_linkSpecificallyExcluded_returnsTrueEvenWithoutGlobalOptOut() {
        // Aucun parent en opt-out global (champ Parent#excludeFromAutoDeduction) — seul le lien
        // élève-parent de CET enfant est exclu individuellement (§retour comptable : enfant payé
        // directement par le parent, contrairement à ses frères/sœurs toujours à charge de l'avantage).
        when(studentParentRepository.findParentsByStudentId(11L)).thenReturn(List.of(
                Parent.builder().id(3L).excludeFromAutoDeduction(false).build()));
        when(studentParentRepository.findLinksByStudentId(11L)).thenReturn(List.of(
                StudentParent.builder().id(100L).studentId(11L).parentId(3L).excludedFromStaffDeduction(true).build()));

        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(11L)).isTrue();
    }

    @Test
    void hasOptedOutOfStaffAutoDeduction_siblingsOfSameStaffParent_canDifferIndependently() {
        // Même parent (id=3L) lié aux deux enfants (cas réel : un seul compte Parent pour le salarié,
        // partagé par sa fratrie) — mais seul l'enfant 11L est exclu individuellement, l'enfant 12L
        // reste normalement à charge de l'avantage personnel.
        when(studentParentRepository.findParentsByStudentId(11L)).thenReturn(List.of(
                Parent.builder().id(3L).excludeFromAutoDeduction(false).build()));
        when(studentParentRepository.findLinksByStudentId(11L)).thenReturn(List.of(
                StudentParent.builder().id(100L).studentId(11L).parentId(3L).excludedFromStaffDeduction(true).build()));
        when(studentParentRepository.findParentsByStudentId(12L)).thenReturn(List.of(
                Parent.builder().id(3L).excludeFromAutoDeduction(false).build()));
        when(studentParentRepository.findLinksByStudentId(12L)).thenReturn(List.of(
                StudentParent.builder().id(101L).studentId(12L).parentId(3L).excludedFromStaffDeduction(false).build()));

        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(11L)).isTrue();
        assertThat(adapter.hasOptedOutOfStaffAutoDeduction(12L)).isFalse();
    }

    @Test
    void setChildStaffDeductionExcluded_byUserId_updatesOnlyMatchingLink() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        StudentParent linkToStaffParent = StudentParent.builder().id(100L).studentId(11L).parentId(3L).excludedFromStaffDeduction(false).build();
        StudentParent linkToOtherParent = StudentParent.builder().id(101L).studentId(11L).parentId(99L).excludedFromStaffDeduction(false).build();
        when(studentParentRepository.findLinksByStudentId(11L)).thenReturn(List.of(linkToStaffParent, linkToOtherParent));

        adapter.setChildStaffDeductionExcluded(7L, null, null, 11L, true);

        ArgumentCaptor<StudentParent> saved = ArgumentCaptor.forClass(StudentParent.class);
        verify(studentParentRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(100L);
        assertThat(saved.getValue().isExcludedFromStaffDeduction()).isTrue();
    }

    @Test
    void setChildStaffDeductionExcluded_byDirectStaffLink_updatesMatchingLink() {
        when(parentRepository.findAllByLinkedStaff(StaffLinkType.TEACHER, 36L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        StudentParent link = StudentParent.builder().id(100L).studentId(11L).parentId(3L).excludedFromStaffDeduction(true).build();
        when(studentParentRepository.findLinksByStudentId(11L)).thenReturn(List.of(link));

        adapter.setChildStaffDeductionExcluded(null, "TEACHER", 36L, 11L, false);

        ArgumentCaptor<StudentParent> saved = ArgumentCaptor.forClass(StudentParent.class);
        verify(studentParentRepository).save(saved.capture());
        assertThat(saved.getValue().isExcludedFromStaffDeduction()).isFalse();
    }

    @Test
    void setChildStaffDeductionExcluded_unknownStaffType_neverThrows_noLinkMatched() {
        when(studentParentRepository.findLinksByStudentId(11L)).thenReturn(List.of(
                StudentParent.builder().id(100L).studentId(11L).parentId(3L).build()));

        adapter.setChildStaffDeductionExcluded(null, "BOGUS", 36L, 11L, true);

        verify(studentParentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setChildStaffDeductionExcluded_nullStudentId_doesNothing() {
        adapter.setChildStaffDeductionExcluded(7L, null, null, null, true);

        verify(studentParentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
