package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.students.application.port.in.EnrollmentUseCase.EnrollCommand;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.students.ClassCapacityPort;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import gn.lacaeducation.students.EnrollmentType;
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
    @Mock StudentRepository studentRepository;
    @InjectMocks InscriptionService service;

    @Test
    void search_excludesEnrollmentsOfArchivedStudents() {
        // §retour comptable : un élève archivé n'a plus rien à gérer activement — son inscription ne
        // doit plus apparaître dans /enrollments, même si la ligne existe toujours en base.
        Enrollment active = Enrollment.builder().id(1L).studentId(7L).classId(3L).build();
        Enrollment archived = Enrollment.builder().id(2L).studentId(8L).classId(3L).build();
        when(enrollmentRepository.search(null, 3L, null, null)).thenReturn(java.util.List.of(active, archived));
        when(studentRepository.existsById(7L)).thenReturn(true);
        when(studentRepository.existsById(8L)).thenReturn(false);

        java.util.List<Enrollment> result = service.search(null, 3L, null, null);

        assertThat(result).containsExactly(active);
    }

    @Test
    void enroll_createsWithPendingStatus() {
        when(enrollmentRepository.save(any())).thenAnswer(i -> { Enrollment e = i.getArgument(0); e.setId(1L); return e; });
        Enrollment result = service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW, null, null));
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void enroll_publishesEnrollmentCreatedEvent() {
        when(enrollmentRepository.save(any())).thenAnswer(i -> { Enrollment e = i.getArgument(0); e.setId(1L); return e; });
        service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW, null, null));
        verify(eventPublisher).publishEvent(any(gn.lacaeducation.students.application.event.EnrollmentCreatedEvent.class));
    }

    @Test
    void enroll_duplicateActiveEnrollmentForYear_throwsConflict() {
        when(enrollmentRepository.existsActiveForStudentYear(10L, 30L)).thenReturn(true);
        assertThatThrownBy(() -> service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void update_nonPendingEnrollment_throwsBusinessRule() {
        Enrollment validated = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.VALIDATED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(validated));
        assertThatThrownBy(() -> service.update(1L, new EnrollCommand(10L, 21L, 30L, EnrollmentType.NEW, null, null)))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
    }

    @Test
    void validatePaidEnrollment_validatesMatchingPendingEnrollment() {
        Enrollment pending = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findPendingByStudentAndClass(10L, 20L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.existsValidatedForStudentYear(10L, 30L)).thenReturn(false);
        when(classCapacityPort.maxCapacity(20L)).thenReturn(null);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.validatePaidEnrollment(10L, 20L);

        assertThat(pending.getStatus()).isEqualTo(EnrollmentStatus.VALIDATED);
        verify(eventPublisher).publishEvent(any(EnrollmentValidatedEvent.class));
    }

    @Test
    void validatePaidEnrollment_noPendingEnrollment_noop() {
        when(enrollmentRepository.findPendingByStudentAndClass(10L, 20L)).thenReturn(Optional.empty());
        service.validatePaidEnrollment(10L, 20L);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void enroll_repositoryError_propagatesException() {
        when(enrollmentRepository.save(any())).thenThrow(new RuntimeException("DB error"));
        assertThatThrownBy(() -> service.enroll(new EnrollCommand(10L, 20L, 30L, EnrollmentType.NEW, null, null)))
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

    @Test
    void abandon_fromValidated_setsStatusAbandonedAndPublishesEvent() {
        Enrollment validated = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.VALIDATED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(validated));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.abandon(1L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ABANDONED);
        ArgumentCaptor<gn.lacaeducation.students.application.event.EnrollmentAbandonedEvent> captor =
                ArgumentCaptor.forClass(gn.lacaeducation.students.application.event.EnrollmentAbandonedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(10L);
        assertThat(captor.getValue().classId()).isEqualTo(20L);
    }

    @Test
    void abandon_fromPending_throwsBusinessRule() {
        Enrollment pending = Enrollment.builder().id(1L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.abandon(1L))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void abandon_fromCancelled_throwsBusinessRule() {
        Enrollment cancelled = Enrollment.builder().id(1L).status(EnrollmentStatus.CANCELLED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.abandon(1L))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void transferClass_fromValidated_changesClassAndPublishesEvent() {
        Enrollment validated = Enrollment.builder().id(1L).studentId(10L).classId(20L)
                .academicYearId(30L).status(EnrollmentStatus.VALIDATED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(validated));
        when(classCapacityPort.maxCapacity(21L)).thenReturn(null);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Enrollment result = service.transferClass(1L, 21L);

        assertThat(result.getClassId()).isEqualTo(21L);
        ArgumentCaptor<gn.lacaeducation.students.application.event.EnrollmentClassChangedEvent> captor =
                ArgumentCaptor.forClass(gn.lacaeducation.students.application.event.EnrollmentClassChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(10L);
        assertThat(captor.getValue().oldClassId()).isEqualTo(20L);
        assertThat(captor.getValue().newClassId()).isEqualTo(21L);
        assertThat(captor.getValue().academicYearId()).isEqualTo(30L);
    }

    @Test
    void transferClass_fromPending_throwsBusinessRule() {
        Enrollment pending = Enrollment.builder().id(1L).classId(20L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.transferClass(1L, 21L))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void transferClass_sameClass_throwsBusinessRule() {
        Enrollment validated = Enrollment.builder().id(1L).classId(20L).status(EnrollmentStatus.VALIDATED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(validated));

        assertThatThrownBy(() -> service.transferClass(1L, 20L))
                .isInstanceOf(gn.lacaeducation.common.BusinessRuleException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void transferClass_targetClassFull_throwsConflict() {
        Enrollment validated = Enrollment.builder().id(1L).classId(20L).status(EnrollmentStatus.VALIDATED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(validated));
        when(classCapacityPort.maxCapacity(21L)).thenReturn(2);
        when(enrollmentRepository.countByClassIdAndStatus(21L, EnrollmentStatus.VALIDATED)).thenReturn(2L);

        assertThatThrownBy(() -> service.transferClass(1L, 21L))
                .isInstanceOf(ConflictException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
