package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentOwnershipGuardTest {

    @Mock AssessmentUseCase assessmentUseCase;
    @Mock TeacherPublicInfoProvider teacherPublicInfoProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @InjectMocks AssessmentOwnershipGuard guard;

    private Authentication teacherAuth() {
        return new TestingAuthenticationToken("prof", null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private Authentication adminAuth() {
        return new TestingAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN")));
    }

    @Test
    void owner_isAllowed() {
        when(assessmentUseCase.findById(9L)).thenReturn(Optional.of(Assessment.builder().id(9L).teacherId(5L).build()));
        when(teacherPublicInfoProvider.findTeacherIdByUserId(100L)).thenReturn(Optional.of(5L));

        assertThatCode(() -> guard.requireOwnerOrAdmin(9L, 100L, teacherAuth())).doesNotThrowAnyException();
    }

    @Test
    void differentTeacher_isDenied() {
        when(assessmentUseCase.findById(9L)).thenReturn(Optional.of(Assessment.builder().id(9L).teacherId(5L).build()));
        when(teacherPublicInfoProvider.findTeacherIdByUserId(100L)).thenReturn(Optional.of(6L));

        assertThatThrownBy(() -> guard.requireOwnerOrAdmin(9L, 100L, teacherAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void notATeacherAtAll_isDenied() {
        when(assessmentUseCase.findById(9L)).thenReturn(Optional.of(Assessment.builder().id(9L).teacherId(5L).build()));
        when(teacherPublicInfoProvider.findTeacherIdByUserId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireOwnerOrAdmin(9L, 100L, teacherAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void schoolAdmin_bypassesOwnershipCheck_evenOnSomeoneElsesAssessment() {
        assertThatCode(() -> guard.requireOwnerOrAdmin(9L, 100L, adminAuth())).doesNotThrowAnyException();
    }

    @Test
    void unknownAssessment_throwsNotFound() {
        when(assessmentUseCase.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireOwnerOrAdmin(9L, 100L, teacherAuth()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── requireAdmin (§retour fondateur : la suppression reste admin-only, même pour le créateur) ──

    @Test
    void requireAdmin_teacher_isDenied_evenAsOwner() {
        assertThatThrownBy(() -> guard.requireAdmin(teacherAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAdmin_schoolAdmin_isAllowed() {
        assertThatCode(() -> guard.requireAdmin(adminAuth())).doesNotThrowAnyException();
    }

    // ── requireAssignedClassSubject (§retour fondateur : pas de classe hors affectation) ───────────

    @Test
    void requireAssignedClassSubject_teacherAssigned_isAllowed() {
        when(teacherPublicInfoProvider.findTeacherIdByUserId(100L)).thenReturn(Optional.of(5L));
        when(academicsDataProvider.findClassSubjectsByTeacher(5L)).thenReturn(List.of(
                new ClassSubjectPublicInfo(1L, 2L, null, 5L, null)));

        assertThatCode(() -> guard.requireAssignedClassSubject(1L, 2L, 100L, teacherAuth())).doesNotThrowAnyException();
    }

    @Test
    void requireAssignedClassSubject_teacherNotAssignedToThatClass_isDenied() {
        when(teacherPublicInfoProvider.findTeacherIdByUserId(100L)).thenReturn(Optional.of(5L));
        when(academicsDataProvider.findClassSubjectsByTeacher(5L)).thenReturn(List.of(
                new ClassSubjectPublicInfo(1L, 2L, null, 5L, null)));

        assertThatThrownBy(() -> guard.requireAssignedClassSubject(99L, 2L, 100L, teacherAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireAssignedClassSubject_schoolAdmin_bypassesCheck() {
        assertThatCode(() -> guard.requireAssignedClassSubject(99L, 2L, 100L, adminAuth())).doesNotThrowAnyException();
    }
}
