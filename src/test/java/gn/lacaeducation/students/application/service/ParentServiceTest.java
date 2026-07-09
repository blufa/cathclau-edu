package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.application.port.in.ParentUseCase.UpdateProfileCommand;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock ParentRepository parentRepository;
    @Mock StudentParentRepository studentParentRepository;
    @Mock StudentRepository studentRepository;
    @InjectMocks ParentService service;

    @Test
    void updateOwnProfile_partial_keepsNameAndUpdatesContact() {
        Parent existing = Parent.builder().id(3L).userId(7L).firstName("Awa").lastName("Diallo")
                .phone("old").email("old@x").address("old").build();
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent updated = service.updateOwnProfile(7L, new UpdateProfileCommand("623000000", "awa@ecole.gn", null, null));

        assertThat(updated.getFirstName()).isEqualTo("Awa");   // nom non modifiable
        assertThat(updated.getLastName()).isEqualTo("Diallo");
        assertThat(updated.getPhone()).isEqualTo("623000000");
        assertThat(updated.getEmail()).isEqualTo("awa@ecole.gn");
        assertThat(updated.getAddress()).isEqualTo("old");      // champ null => inchangé
    }

    @Test
    void updateOwnPhoto_setsPhoto() {
        Parent existing = Parent.builder().id(3L).userId(7L).build();
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent updated = service.updateOwnPhoto(7L, "https://cdn/x.jpg");

        assertThat(updated.getPhoto()).isEqualTo("https://cdn/x.jpg");
    }

    @Test
    void updateOwnProfile_noParentForUser_throwsNotFound() {
        when(parentRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateOwnProfile(7L, new UpdateProfileCommand("x", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
