package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.QuotaExceededException;
import gn.lacaeducation.platform.TenantQuotaService;
import gn.lacaeducation.students.application.event.StudentCreatedEvent;
import gn.lacaeducation.students.application.port.in.StudentUseCase.CreateStudentCommand;
import gn.lacaeducation.students.application.port.out.StudentDocumentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock StudentDocumentRepository documentRepository;
    @Mock TenantQuotaService tenantQuotaService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock gn.lacaeducation.common.TenantInfoProvider tenantInfoProvider;
    @Mock gn.lacaeducation.students.PortalAccountProvisioner portalAccountProvisioner;
    @InjectMocks EleveService service;

    private CreateStudentCommand cmd(String number) {
        return new CreateStudentCommand(10L, null, number, "Mariama", "Bah",
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                null, null);
    }

    @Test
    void create_quotaExceeded_throws403() {
        when(studentRepository.countByTenantId(10L)).thenReturn(500L);
        when(tenantQuotaService.isQuotaAvailable(10L, 500)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd("2026-1")))
                .isInstanceOf(QuotaExceededException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_autoGeneratesMatricule_whenBlank() {
        when(studentRepository.countByTenantId(10L)).thenReturn(41L);
        when(tenantQuotaService.isQuotaAvailable(10L, 41)).thenReturn(true);
        when(studentRepository.existsByTenantIdAndStudentNumber(any(), any())).thenReturn(false);
        when(studentRepository.save(any())).thenAnswer(i -> { Student s = i.getArgument(0); s.setId(7L); return s; });

        Student result = service.create(cmd(null));

        // Matricule auto-généré au format simple <sigleÉcole><séquence>, ex. EC00042.
        assertThat(result.getStudentNumber()).matches("[A-Z]{2,6}\\d{5}");
        verify(eventPublisher).publishEvent(any(StudentCreatedEvent.class));
    }

    @Test
    void create_duplicateProvidedMatricule_throwsConflict() {
        when(studentRepository.countByTenantId(10L)).thenReturn(1L);
        when(tenantQuotaService.isQuotaAvailable(10L, 1)).thenReturn(true);
        when(studentRepository.existsByTenantIdAndStudentNumber(10L, "DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd("DUP")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_softDeletesStudent() {
        Student s = Student.builder().id(7L).tenantId(10L).build();
        when(studentRepository.findById(7L)).thenReturn(Optional.of(s));
        when(studentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(7L);

        assertThat(s.getDeletedAt()).isNotNull();
        verify(studentRepository).save(s);
    }
}
