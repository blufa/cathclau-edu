package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.students.application.port.in.ParentUseCase.CreateParentCommand;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(existing));
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
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent updated = service.updateOwnPhoto(7L, "https://cdn/x.jpg");

        assertThat(updated.getPhoto()).isEqualTo("https://cdn/x.jpg");
    }

    @Test
    void updateOwnProfile_noParentForUser_throwsNotFound() {
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateOwnProfile(7L, new UpdateProfileCommand("x", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUserId_duplicateRows_returnsOldestAsCanonical() {
        // Fiches dupliquées (même compte lié séparément à chaque enfant) : la plus ancienne (id le plus
        // bas) est retenue comme fiche "canonique" pour le profil/portail parent.
        Parent oldest = Parent.builder().id(3L).userId(7L).firstName("Fama").build();
        Parent duplicate = Parent.builder().id(9L).userId(7L).firstName("Fama").build();
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(duplicate, oldest));

        Optional<Parent> result = service.findByUserId(7L);

        assertThat(result).contains(oldest);
    }

    @Test
    void create_calledTwiceWithSameIdempotencyKey_returnsSameParent_doesNotDuplicate() {
        // Rejoué après une coupure réseau (mode hors-ligne, §students) avec la même clé : le second
        // appel doit renvoyer le parent déjà créé, sans en créer un second.
        Parent alreadyCreated = Parent.builder().id(9L).tenantId(10L).firstName("Awa").lastName("Diallo")
                .idempotencyKey("key-1").build();
        when(parentRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(alreadyCreated));
        when(parentRepository.save(any())).thenAnswer(i -> {
            Parent p = i.getArgument(0);
            if (p.getId() == null) p.setId(9L);
            return p;
        });

        CreateParentCommand cmd = new CreateParentCommand(10L, null, "Awa", "Diallo", "620000000",
                null, null, null, "key-1");

        Parent first = service.create(cmd);
        Parent second = service.create(cmd);

        assertThat(first.getId()).isEqualTo(second.getId());
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void create_sameUserIdSameTenant_reusesExistingParent_doesNotDuplicate() {
        // Personnel de l'école inscrivant un 2e enfant : le même compte (userId) ne doit pas créer une
        // 2e fiche Parent — bug réel qui faisait planter la génération de fiche de paie (findByUserId
        // renvoyant plusieurs résultats).
        Parent existing = Parent.builder().id(3L).tenantId(10L).userId(77L).firstName("Fama").lastName("Millimouno").build();
        when(parentRepository.findAllByUserId(77L)).thenReturn(List.of(existing));

        CreateParentCommand cmd = new CreateParentCommand(10L, 77L, "Fama", "Millimouno", "620000001",
                null, null, null, null);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(3L);
        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void create_sameUserIdDifferentTenant_createsNewParent() {
        Parent existingOtherTenant = Parent.builder().id(3L).tenantId(99L).userId(77L).build();
        when(parentRepository.findAllByUserId(77L)).thenReturn(List.of(existingOtherTenant));
        when(parentRepository.save(any())).thenAnswer(i -> {
            Parent p = i.getArgument(0);
            p.setId(15L);
            return p;
        });

        CreateParentCommand cmd = new CreateParentCommand(10L, 77L, "Fama", "Millimouno", "620000001",
                null, null, null, null);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(15L);
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void findStudentsByUserId_duplicateParentRows_aggregatesChildrenAcrossAll() {
        Parent p1 = Parent.builder().id(3L).userId(7L).build();
        Parent p2 = Parent.builder().id(5L).userId(7L).build();
        when(parentRepository.findAllByUserId(7L)).thenReturn(List.of(p1, p2));
        when(studentParentRepository.findStudentIdsByParentId(3L)).thenReturn(List.of(10L));
        when(studentParentRepository.findStudentIdsByParentId(5L)).thenReturn(List.of(11L));
        when(studentRepository.findById(10L)).thenReturn(Optional.of(
                gn.lacaeducation.students.domain.Student.builder().id(10L).build()));
        when(studentRepository.findById(11L)).thenReturn(Optional.of(
                gn.lacaeducation.students.domain.Student.builder().id(11L).build()));

        List<gn.lacaeducation.students.domain.Student> result = service.findStudentsByUserId(7L);

        assertThat(result).extracting(gn.lacaeducation.students.domain.Student::getId)
                .containsExactlyInAnyOrder(10L, 11L);
    }
}
