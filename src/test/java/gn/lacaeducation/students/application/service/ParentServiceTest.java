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
                null, null, null, "key-1", null, null);

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
                null, null, null, null, null, null);

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
                null, null, null, null, null, null);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(15L);
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void create_sameStaffLinkSameTenant_reusesExistingParent_doesNotDuplicate() {
        // Personnel SANS compte (ex. pas de smartphone Android) inscrivant un 2e enfant : le même lien
        // direct (staffType/staffId) ne doit pas créer une 2e fiche Parent.
        Parent existing = Parent.builder().id(4L).tenantId(10L)
                .linkedStaffType(gn.lacaeducation.students.domain.StaffLinkType.TEACHER).linkedStaffId(55L).build();
        when(parentRepository.findAllByLinkedStaff(gn.lacaeducation.students.domain.StaffLinkType.TEACHER, 55L))
                .thenReturn(List.of(existing));

        CreateParentCommand cmd = new CreateParentCommand(10L, null, "Fama", "Millimouno", "620000001",
                null, null, null, null, gn.lacaeducation.students.domain.StaffLinkType.TEACHER, 55L);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(4L);
        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void create_samePhoneSameTenant_reusesExistingParent_doesNotDuplicate() {
        // Retour comptable direct : une coupure réseau pendant l'inscription d'un parent (à
        // l'inscription d'un enfant) laisse croire à l'agent que la création a échoué alors qu'elle a
        // réussi côté serveur — il ressaisit le même parent (même numéro), ce qui créait une 2e fiche.
        Parent existing = Parent.builder().id(5L).tenantId(10L).firstName("Mamadou").lastName("Bah")
                .phone("+224620000002").build();
        when(parentRepository.findByTenantIdAndPhone(10L, "620000002")).thenReturn(Optional.empty());
        when(parentRepository.findByTenantIdAndPhone(10L, "+224620000002")).thenReturn(Optional.of(existing));

        CreateParentCommand cmd = new CreateParentCommand(10L, null, "Mamadou", "Bah", "620000002",
                null, null, null, null, null, null);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(5L);
        verify(parentRepository, never()).save(any(Parent.class));
    }

    @Test
    void create_newPhoneNotTaken_createsNewParent() {
        when(parentRepository.findByTenantIdAndPhone(any(), any())).thenReturn(Optional.empty());
        when(parentRepository.save(any())).thenAnswer(i -> {
            Parent p = i.getArgument(0);
            p.setId(20L);
            return p;
        });

        CreateParentCommand cmd = new CreateParentCommand(10L, null, "Oumar", "Sylla", "620000099",
                null, null, null, null, null, null);

        Parent result = service.create(cmd);

        assertThat(result.getId()).isEqualTo(20L);
        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void delete_purgesStudentParentLinksBeforeDeletingTheParent() {
        // §retour comptable : supprimer un parent (ex. doublon créé par une coupure réseau) ne doit
        // jamais laisser de lien élève-parent orphelin (pas de contrainte FK en base sur
        // student_parents) — purge d'abord les liens, jamais les fiches Student elles-mêmes.
        service.delete(5L);

        verify(studentParentRepository, times(1)).unlinkAllByParentId(5L);
        verify(parentRepository, times(1)).deleteById(5L);
    }

    @Test
    void update_withUserId_linksAccountWithoutClearingOnNull() {
        Parent existing = Parent.builder().id(1L).firstName("Awa").lastName("Diallo").userId(null).build();
        when(parentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent result = service.update(1L, new gn.lacaeducation.students.application.port.in.ParentUseCase.UpdateParentCommand(
                "Awa", "Diallo", "620000000", null, null, null, 99L, null, null, false));

        assertThat(result.getUserId()).isEqualTo(99L);
    }

    @Test
    void update_withStaffLink_linksDirectlyWithoutAccount() {
        Parent existing = Parent.builder().id(1L).firstName("Fama").lastName("Millimouno").build();
        when(parentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent result = service.update(1L, new gn.lacaeducation.students.application.port.in.ParentUseCase.UpdateParentCommand(
                "Fama", "Millimouno", "620000001", null, null, null, null,
                gn.lacaeducation.students.domain.StaffLinkType.EMPLOYEE, 42L, false));

        assertThat(result.getLinkedStaffType()).isEqualTo(gn.lacaeducation.students.domain.StaffLinkType.EMPLOYEE);
        assertThat(result.getLinkedStaffId()).isEqualTo(42L);
    }

    @Test
    void update_excludeFromAutoDeduction_persistsFlag() {
        // Retour comptable : un enseignant/employé qui paie réellement lui-même les frais de son enfant
        // doit pouvoir être exclu de toute retenue automatique sur salaire via ce réglage.
        Parent existing = Parent.builder().id(1L).firstName("Awa").lastName("Diallo").excludeFromAutoDeduction(false).build();
        when(parentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Parent result = service.update(1L, new gn.lacaeducation.students.application.port.in.ParentUseCase.UpdateParentCommand(
                "Awa", "Diallo", "620000000", null, null, null, null, null, null, true));

        assertThat(result.isExcludeFromAutoDeduction()).isTrue();
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

    @Test
    void findLinkedParentIds_excludesLinkedIdsFromOtherTenantsAndUnlinkedParents() {
        // Parent 1 et 2 appartiennent à cette école ; parent 2 est un prospect sans enfant ; parent 99
        // a un lien enfant mais appartient à une AUTRE école (ne doit jamais apparaître ici).
        Parent p1 = Parent.builder().id(1L).tenantId(50L).build();
        Parent p2 = Parent.builder().id(2L).tenantId(50L).build();
        when(parentRepository.findByTenantId(50L)).thenReturn(List.of(p1, p2));
        when(studentParentRepository.findDistinctLinkedParentIds()).thenReturn(List.of(1L, 99L));

        List<Long> result = service.findLinkedParentIds(50L);

        assertThat(result).containsExactly(1L);
    }
}
