package gn.lacaeducation.dpe.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase;
import gn.lacaeducation.dpe.application.port.in.DpeDashboardUseCase.SchoolSummary;
import gn.lacaeducation.dpe.application.port.out.DpeAlertRepository;
import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DpeAlertServiceTest {

    @Mock DpeAlertRepository repository;
    @Mock DpeDashboardUseCase dashboardUseCase;

    DpeAlertService service() {
        return new DpeAlertService(repository, dashboardUseCase);
    }

    private SchoolSummary healthySchool(Long tenantId) {
        return new SchoolSummary(tenantId, "École OK", "Coyah", "ACTIVE", 100, 5, 5, 4);
    }

    @Test
    void evaluateForPrefecture_createsNoAlerts_forHealthySchool() {
        when(dashboardUseCase.schools(Set.of(1L))).thenReturn(List.of(healthySchool(1L)));
        when(repository.findByPrefectureIdAndTenantIdAndType(any(), any(), any())).thenReturn(Optional.empty());

        service().evaluateForPrefecture(10L, Set.of(1L));

        verify(repository, never()).save(any());
    }

    @Test
    void evaluateForPrefecture_createsAlert_whenSchoolHasNoStudents() {
        SchoolSummary noStudents = new SchoolSummary(1L, "École Vide", "Coyah", "ACTIVE", 0, 5, 5, 4);
        when(dashboardUseCase.schools(Set.of(1L))).thenReturn(List.of(noStudents));
        when(repository.findByPrefectureIdAndTenantIdAndType(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(DpeAlert.class))).thenAnswer(i -> i.getArgument(0));

        service().evaluateForPrefecture(10L, Set.of(1L));

        ArgumentCaptor<DpeAlert> captor = ArgumentCaptor.forClass(DpeAlert.class);
        verify(repository).save(captor.capture());
        DpeAlert saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(DpeAlertType.SCHOOL_NO_STUDENTS);
        assertThat(saved.getStatut()).isEqualTo(DpeAlertStatus.NOUVELLE);
        assertThat(saved.getPrefectureId()).isEqualTo(10L);
        assertThat(saved.getTenantId()).isEqualTo(1L);
    }

    @Test
    void evaluateForPrefecture_flagsHighStudentTeacherRatio() {
        SchoolSummary overloaded = new SchoolSummary(1L, "École Surchargée", "Coyah", "ACTIVE", 500, 5, 5, 10);
        when(dashboardUseCase.schools(Set.of(1L))).thenReturn(List.of(overloaded));
        when(repository.findByPrefectureIdAndTenantIdAndType(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(DpeAlert.class))).thenAnswer(i -> i.getArgument(0));

        service().evaluateForPrefecture(10L, Set.of(1L));

        ArgumentCaptor<DpeAlert> captor = ArgumentCaptor.forClass(DpeAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(DpeAlertType.HIGH_STUDENT_TEACHER_RATIO);
    }

    @Test
    void evaluateForPrefecture_autoResolvesAlert_whenConditionNoLongerTrue() {
        DpeAlert existingOpen = DpeAlert.builder().id(1L).prefectureId(10L).tenantId(1L)
                .type(DpeAlertType.SCHOOL_NO_STUDENTS).statut(DpeAlertStatus.EN_COURS).build();
        when(dashboardUseCase.schools(Set.of(1L))).thenReturn(List.of(healthySchool(1L)));
        // Défaut : aucune alerte existante pour les autres règles, écrasé ci-dessous pour la règle testée.
        lenient().when(repository.findByPrefectureIdAndTenantIdAndType(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.findByPrefectureIdAndTenantIdAndType(10L, 1L, DpeAlertType.SCHOOL_NO_STUDENTS))
                .thenReturn(Optional.of(existingOpen));
        when(repository.save(any(DpeAlert.class))).thenAnswer(i -> i.getArgument(0));

        service().evaluateForPrefecture(10L, Set.of(1L));

        assertThat(existingOpen.getStatut()).isEqualTo(DpeAlertStatus.RESOLUE);
        assertThat(existingOpen.getResolvedAt()).isNotNull();
    }

    @Test
    void evaluateForPrefecture_reopensResolvedAlert_whenConditionReappears() {
        DpeAlert resolved = DpeAlert.builder().id(1L).prefectureId(10L).tenantId(1L)
                .type(DpeAlertType.SCHOOL_NO_STUDENTS).statut(DpeAlertStatus.RESOLUE)
                .resolvedAt(LocalDateTime.now().minusDays(1)).build();
        SchoolSummary noStudents = new SchoolSummary(1L, "École Vide", "Coyah", "ACTIVE", 0, 5, 5, 4);
        when(dashboardUseCase.schools(Set.of(1L))).thenReturn(List.of(noStudents));
        lenient().when(repository.findByPrefectureIdAndTenantIdAndType(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.findByPrefectureIdAndTenantIdAndType(10L, 1L, DpeAlertType.SCHOOL_NO_STUDENTS))
                .thenReturn(Optional.of(resolved));
        when(repository.save(any(DpeAlert.class))).thenAnswer(i -> i.getArgument(0));

        service().evaluateForPrefecture(10L, Set.of(1L));

        assertThat(resolved.getStatut()).isEqualTo(DpeAlertStatus.NOUVELLE);
        assertThat(resolved.getResolvedAt()).isNull();
    }

    @Test
    void updateStatus_deniesAccess_whenAlertBelongsToDifferentPrefecture() {
        DpeAlert alert = DpeAlert.builder().id(1L).prefectureId(99L).statut(DpeAlertStatus.NOUVELLE).build();
        when(repository.findById(1L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service().updateStatus(10L, 1L, DpeAlertStatus.EN_COURS, 7L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_unknownAlert_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().updateStatus(10L, 99L, DpeAlertStatus.EN_COURS, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_toResolue_stampsResolvedAtAndUser() {
        DpeAlert alert = DpeAlert.builder().id(1L).prefectureId(10L).statut(DpeAlertStatus.EN_COURS).build();
        when(repository.findById(1L)).thenReturn(Optional.of(alert));
        when(repository.save(any(DpeAlert.class))).thenAnswer(i -> i.getArgument(0));

        DpeAlert result = service().updateStatus(10L, 1L, DpeAlertStatus.RESOLUE, 7L);

        assertThat(result.getStatut()).isEqualTo(DpeAlertStatus.RESOLUE);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.getResolvedByUserId()).isEqualTo(7L);
    }

    @Test
    void updateStatus_illegalTransition_throwsBusinessRuleException() {
        // EN_COURS ne peut revenir qu'à RESOLUE, jamais directement à NOUVELLE.
        DpeAlert alert = DpeAlert.builder().id(1L).prefectureId(10L).statut(DpeAlertStatus.EN_COURS).build();
        when(repository.findById(1L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service().updateStatus(10L, 1L, DpeAlertStatus.NOUVELLE, 7L))
                .isInstanceOf(BusinessRuleException.class);
    }
}
