package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.StaffLinkType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void findChildStudentIdsWithValidatedEnrollment_excludesChildrenWithoutValidatedEnrollment() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(Parent.builder().id(3L).build()));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L, 11L));
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(
                List.of(Enrollment.builder().studentId(10L).status(EnrollmentStatus.VALIDATED).build()));
        when(enrollmentRepository.findByStudentId(11L)).thenReturn(
                List.of(Enrollment.builder().studentId(11L).status(EnrollmentStatus.ABANDONED).build()));

        assertThat(adapter.findChildStudentIdsWithValidatedEnrollment(7L)).containsExactly(10L);
    }

    @Test
    void findChildStudentIdsWithValidatedEnrollment_notAParent_returnsEmpty() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of());

        assertThat(adapter.findChildStudentIdsWithValidatedEnrollment(7L)).isEmpty();
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
    void findChildStudentIdsWithValidatedEnrollmentByStaffLink_resolvesViaDirectLink_excludesUnvalidated() {
        // Personnel SANS compte de connexion (lien direct staffType/staffId au lieu de userId) :
        // régression découverte en vrai — la retenue sur salaire ne trouvait jamais ces enfants.
        when(parentRepository.findAllByLinkedStaff(StaffLinkType.TEACHER, 36L))
                .thenReturn(List.of(Parent.builder().id(59L).build()));
        when(studentParentRepository.findStudentIdsByParentId(59L)).thenReturn(List.of(137L, 138L));
        when(enrollmentRepository.findByStudentId(137L)).thenReturn(
                List.of(Enrollment.builder().studentId(137L).status(EnrollmentStatus.VALIDATED).build()));
        when(enrollmentRepository.findByStudentId(138L)).thenReturn(
                List.of(Enrollment.builder().studentId(138L).status(EnrollmentStatus.PENDING).build()));

        assertThat(adapter.findChildStudentIdsWithValidatedEnrollmentByStaffLink("TEACHER", 36L)).containsExactly(137L);
    }

    @Test
    void findChildStudentIdsWithValidatedEnrollmentByStaffLink_nullArgs_returnsEmpty() {
        assertThat(adapter.findChildStudentIdsWithValidatedEnrollmentByStaffLink(null, 36L)).isEmpty();
        assertThat(adapter.findChildStudentIdsWithValidatedEnrollmentByStaffLink("TEACHER", null)).isEmpty();
    }

    @Test
    void findChildStudentIdsWithValidatedEnrollmentByStaffLink_unknownStaffType_returnsEmpty() {
        assertThat(adapter.findChildStudentIdsWithValidatedEnrollmentByStaffLink("BOGUS", 36L)).isEmpty();
    }
}
