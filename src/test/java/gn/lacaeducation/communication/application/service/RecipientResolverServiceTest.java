package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.EmployeePublicInfoProvider.EmployeeInfo;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipientResolverServiceTest {

    @Mock StudentDataProvider studentDataProvider;
    @Mock TeacherPublicInfoProvider teacherPublicInfoProvider;
    @Mock EmployeePublicInfoProvider employeePublicInfoProvider;
    @InjectMocks RecipientResolverService resolver;

    private static StudentPublicInfo student(long id, String gender) {
        return new StudentPublicInfo(id, 1L, "P", "N", null, null, "MAT-" + id, null, null, gender);
    }

    @Test
    void allParents_returnsEveryParentPhone() {
        when(studentDataProvider.findAllParentsWithPlatformAccess(1L)).thenReturn(List.of(
                new ParentPublicInfo(10L, "Awa", "Diallo", null, "623000001"),
                new ParentPublicInfo(11L, "Ibrahima", "Bah", null, "623000002")));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL_PARENTS, null, null, CommunicationChannel.SMS);

        assertThat(targets).extracting(RecipientTarget::destination).containsExactlyInAnyOrder("623000001", "623000002");
    }

    @Test
    void classParents_dedupesParentWithMultipleChildrenInClass() {
        StudentPublicInfo s1 = student(1L, "MALE");
        StudentPublicInfo s2 = student(2L, "FEMALE");
        when(studentDataProvider.findStudentsByClass(5L)).thenReturn(List.of(s1, s2));
        when(studentDataProvider.hasPlatformAccessOptIn(1L)).thenReturn(true);
        when(studentDataProvider.hasPlatformAccessOptIn(2L)).thenReturn(true);
        ParentPublicInfo sameParent = new ParentPublicInfo(10L, "Awa", "Diallo", null, "623000001");
        when(studentDataProvider.findParentsByStudent(1L)).thenReturn(List.of(sameParent));
        when(studentDataProvider.findParentsByStudent(2L)).thenReturn(List.of(sameParent));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.CLASS_PARENTS, 5L, null, CommunicationChannel.SMS);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).destination()).isEqualTo("623000001");
    }

    @Test
    void classParentsBoys_filtersOnGender() {
        StudentPublicInfo boy = student(1L, "MALE");
        StudentPublicInfo girl = student(2L, "FEMALE");
        when(studentDataProvider.findStudentsByClass(5L)).thenReturn(List.of(boy, girl));
        when(studentDataProvider.hasPlatformAccessOptIn(1L)).thenReturn(true);
        when(studentDataProvider.findParentsByStudent(1L)).thenReturn(List.of(new ParentPublicInfo(10L, "A", "B", null, "623000001")));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.CLASS_PARENTS_BOYS, 5L, null, CommunicationChannel.SMS);

        assertThat(targets).extracting(RecipientTarget::destination).containsExactly("623000001");
    }

    @Test
    void studentParents_resolvesViaStudentDataProvider() {
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(true);
        when(studentDataProvider.findParentsByStudent(7L)).thenReturn(List.of(
                new ParentPublicInfo(10L, "A", "B", null, "623000001")));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.STUDENT_PARENTS, null, 7L, CommunicationChannel.SMS);

        assertThat(targets).extracting(RecipientTarget::destination).containsExactly("623000001");
    }

    @Test
    void studentParents_withoutPlatformAccess_returnsEmpty() {
        when(studentDataProvider.hasPlatformAccessOptIn(7L)).thenReturn(false);

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.STUDENT_PARENTS, null, 7L, CommunicationChannel.SMS);

        assertThat(targets).isEmpty();
    }

    @Test
    void allTeachers_usesTeacherProvider() {
        when(teacherPublicInfoProvider.listTeachers(1L)).thenReturn(List.of(
                new TeacherInfo(20L, "Mariam", "Camara", "ENS-1", "Maths", "mariam@ecole.gn", "623000099")));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL_TEACHERS, null, null, CommunicationChannel.SMS);

        assertThat(targets).extracting(RecipientTarget::destination).containsExactly("623000099");
    }

    @Test
    void allStaff_combinesTeachersAndActiveEmployees() {
        when(teacherPublicInfoProvider.listTeachers(1L)).thenReturn(List.of(
                new TeacherInfo(20L, "Mariam", "Camara", "ENS-1", "Maths", "mariam@ecole.gn", "623000099")));
        when(employeePublicInfoProvider.listEmployees(1L)).thenReturn(List.of(
                new EmployeeInfo(30L, "Aissatou Bah", "COMPTABLE", BigDecimal.TEN, true, "623000088", null),
                new EmployeeInfo(31L, "Ancien Employé", "GARDIEN", BigDecimal.ONE, false, "623000077", null)));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL_STAFF, null, null, CommunicationChannel.SMS);

        // L'employé inactif (31L) est exclu ; seuls l'enseignant et l'employé actif reçoivent le SMS.
        assertThat(targets).extracting(RecipientTarget::destination)
                .containsExactlyInAnyOrder("623000099", "623000088");
    }

    @Test
    void all_combinesParentsTeachersAndStaff_dedupedByDestination() {
        when(studentDataProvider.findAllParentsWithPlatformAccess(1L)).thenReturn(List.of(
                new ParentPublicInfo(10L, "Awa", "Diallo", null, "623000001")));
        when(teacherPublicInfoProvider.listTeachers(1L)).thenReturn(List.of(
                new TeacherInfo(20L, "Mariam", "Camara", "ENS-1", "Maths", "mariam@ecole.gn", "623000099"),
                // Un enseignant qui a aussi le même numéro qu'un parent (ex. l'un des deux) ne doit
                // apparaître qu'une seule fois dans le résultat final.
                new TeacherInfo(21L, "Doublon", "Numero", "ENS-2", "Français", null, "623000001")));
        when(employeePublicInfoProvider.listEmployees(1L)).thenReturn(List.of(
                new EmployeeInfo(30L, "Aissatou Bah", "COMPTABLE", BigDecimal.TEN, true, "623000088", null)));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL, null, null, CommunicationChannel.SMS);

        assertThat(targets).extracting(RecipientTarget::destination)
                .containsExactlyInAnyOrder("623000001", "623000099", "623000088");
    }

    @Test
    void emailChannel_usesEmailInsteadOfPhone() {
        when(studentDataProvider.findAllParentsWithPlatformAccess(1L)).thenReturn(List.of(
                new ParentPublicInfo(10L, "Awa", "Diallo", "awa@ecole.gn", "623000001")));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL_PARENTS, null, null, CommunicationChannel.EMAIL);

        assertThat(targets).extracting(RecipientTarget::destination).containsExactly("awa@ecole.gn");
    }

    @Test
    void ignoresRecipientsWithoutDestination() {
        when(studentDataProvider.findAllParentsWithPlatformAccess(1L)).thenReturn(List.of(
                new ParentPublicInfo(10L, "Awa", "Diallo", null, null)));

        List<RecipientTarget> targets = resolver.resolve(1L, BroadcastAudience.ALL_PARENTS, null, null, CommunicationChannel.SMS);

        assertThat(targets).isEmpty();
    }
}
