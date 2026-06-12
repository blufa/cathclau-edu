package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase.EnrollCommand;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.students.ClassCapacityPort;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.domain.EnrollmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    @Mock EnrollmentRepository enrollmentRepository;
    @Mock ClassCapacityPort classCapacityPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks InscriptionService service;

    @Test
    void enroll_createsWithPendingStatus() {
        when(enrollmentRepository.save(any())).thenAnswer(i -> { Enrollment e = i.getArgument(0); e.setId(1L); return e; });
        Enrollment result = service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW));
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void enroll_repositoryError_propagatesException() {
        when(enrollmentRepository.save(any())).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validate_publishesEnrollmentValidatedEvent() {
        Enrollment pending = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.existsValidatedForStudentYear(10L, 30L)).thenReturn(false);
        when(classCapacityPort.maxCapacity(20L)).thenReturn(null);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.validate(1L);

        ArgumentCaptor<EnrollmentValidatedEvent> captor = ArgumentCaptor.forClass(EnrollmentValidatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(10L);
        assertThat(captor.getValue().classId()).isEqualTo(20L);
    }

    @Test
    void validate_classFull_throwsConflict() {
        Enrollment pending = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.existsValidatedForStudentYear(10L, 30L)).thenReturn(false);
        when(classCapacityPort.maxCapacity(20L)).thenReturn(2);
        when(enrollmentRepository.countByClassIdAndStatus(20L, EnrollmentStatus.VALIDATED)).thenReturn(2L);

        assertThatThrownBy(() -> service.validate(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("capacity");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void validate_studentAlreadyValidatedForYear_throwsConflict() {
        Enrollment pending = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.existsValidatedForStudentYear(10L, 30L)).thenReturn(true);

        assertThatThrownBy(() -> service.validate(1L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_setsStatusCancelled() {
        Enrollment pending = Enrollment.builder().id(1L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.cancel(1L);
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(gn.lacaeducation.students.application.event.EnrollmentCancelledEvent.class));
    }
}
