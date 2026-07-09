package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.grading.GradingTeacherProvider;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase.CreateTeacherCommand;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnseignantServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock GradingTeacherProvider gradingTeacherProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks EnseignantService service;

    @Test
    void create_teacher_savesAndPublishesEvent() {
        Teacher expected = Teacher.builder().id(1L).tenantId(10L).staffNumber("T001").build();
        when(teacherRepository.existsByTenantIdAndStaffNumber(10L, "T001")).thenReturn(false);
        when(teacherRepository.save(any())).thenReturn(expected);

        Teacher result = service.create(new CreateTeacherCommand(10L, null, "T001", "John", "Doe",
                "0600000000", "john@school.gn", "Mathematics", LocalDate.of(2024, 1, 15), null, null, null, null, null, null));

        assertThat(result.getId()).isEqualTo(1L);
        verify(teacherRepository).save(any(Teacher.class));
        verify(eventPublisher).publishEvent(any(gn.lacaeducation.staff.application.event.StaffCreatedEvent.class));
    }

    @Test
    void create_duplicateStaffNumber_throwsConflict() {
        when(teacherRepository.existsByTenantIdAndStaffNumber(10L, "T001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTeacherCommand(10L, null, "T001", "John", "Doe",
                null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void delete_withEvaluations_forbidden() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(Teacher.builder().id(1L).build()));
        when(gradingTeacherProvider.teacherHasEvaluations(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(teacherRepository, never()).deleteById(any());
    }

    @Test
    void delete_withoutEvaluations_deletes() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(Teacher.builder().id(1L).build()));
        when(gradingTeacherProvider.teacherHasEvaluations(1L)).thenReturn(false);

        service.delete(1L);

        verify(teacherRepository).deleteById(1L);
    }

    @Test
    void setActive_deactivatesTeacher() {
        Teacher t = Teacher.builder().id(1L).active(true).build();
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Teacher result = service.setActive(1L, false);

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void findByTenantId_returnsAllForTenant() {
        when(teacherRepository.findByTenantId(10L))
                .thenReturn(List.of(Teacher.builder().id(1L).build(), Teacher.builder().id(2L).build()));
        assertThat(service.findByTenantId(10L)).hasSize(2);
    }
}
