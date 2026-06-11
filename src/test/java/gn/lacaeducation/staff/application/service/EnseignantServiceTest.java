package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.staff.application.port.in.TeacherUseCase.CreateTeacherCommand;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnseignantServiceTest {

    @Mock TeacherRepository teacherRepository;
    @InjectMocks EnseignantService service;

    @Test
    void create_teacher_savesAndReturns() {
        Teacher expected = Teacher.builder().id(1L).tenantId(10L).firstName("John").lastName("Doe").build();
        when(teacherRepository.save(any())).thenReturn(expected);
        Teacher result = service.create(new CreateTeacherCommand(10L, null, "T001", "John", "Doe",
                "0600000000", "john@school.gn", "Mathematics", LocalDate.of(2024, 1, 15)));
        assertThat(result.getId()).isEqualTo(1L);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void findById_existing_returnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(Teacher.builder().id(1L).build()));
        assertThat(service.findById(1L)).isPresent();
    }

    @Test
    void findByTenantId_returnsAllForTenant() {
        when(teacherRepository.findByTenantId(10L))
                .thenReturn(List.of(Teacher.builder().id(1L).build(), Teacher.builder().id(2L).build()));
        assertThat(service.findByTenantId(10L)).hasSize(2);
    }
}
