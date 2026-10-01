package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentDirectoryPort;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDirectoryServiceTest {

    @Mock StudentDirectoryPort directory;
    @Mock StudentRepository studentRepository;
    @Mock ParentRepository parentRepository;
    @InjectMocks StudentDirectoryService service;

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    // ── searchPreviousClasses (§retour comptable : suggestion "dernière classe" dès la 1ère lettre) ──

    @Test
    void searchPreviousClasses_lowercasesAndTrimsBeforeDelegating() {
        when(directory.searchPreviousClasses("cm2")).thenReturn(List.of("CM2"));

        List<String> result = service.searchPreviousClasses("  CM2  ");

        assertThat(result).containsExactly("CM2");
        verify(directory).searchPreviousClasses("cm2");
    }

    @Test
    void searchPreviousClasses_singleLetter_stillDelegates() {
        // Contrairement à searchSchools (2 caractères mini), une seule lettre suffit ici — la
        // suggestion doit apparaître dès le premier caractère tapé.
        when(directory.searchPreviousClasses("c")).thenReturn(List.of("CM2", "CE1"));

        assertThat(service.searchPreviousClasses("c")).containsExactly("CM2", "CE1");
    }

    @Test
    void searchPreviousClasses_blankQuery_returnsEmptyWithoutCallingPort() {
        assertThat(service.searchPreviousClasses("")).isEmpty();
        assertThat(service.searchPreviousClasses(null)).isEmpty();
        verify(directory, never()).searchPreviousClasses(org.mockito.ArgumentMatchers.any());
    }

    // ── searchSchools (comportement existant, non couvert jusqu'ici) ──────────────────────────────

    @Test
    void searchSchools_requiresAtLeastTwoCharacters() {
        assertThat(service.searchSchools("a")).isEmpty();
        verify(directory, never()).searchSchools(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void searchSchools_delegatesLowercasedQuery() {
        when(directory.searchSchools("sainte")).thenReturn(List.of("École Sainte Marie"));

        assertThat(service.searchSchools("Sainte")).containsExactly("École Sainte Marie");
    }

    // ── suggestLastNames / suggestAddresses / suggestFirstNames (§retour comptable : suggestion
    // nom/adresse déjà saisis pour un autre élève OU parent du MÊME tenant, fratries et quartiers) ──

    @Test
    void suggestLastNames_mergesStudentAndParentResults_dedupedAndSorted() {
        TenantContext.set(7L);
        when(studentRepository.suggestLastNames(7L, "cam", 25)).thenReturn(List.of("Camara", "Camus"));
        when(parentRepository.suggestLastNames(7L, "cam", 25)).thenReturn(List.of("Camara", "Cambier"));

        assertThat(service.suggestLastNames("  CAM  ")).containsExactly("Camara", "Cambier", "Camus");
    }

    @Test
    void suggestAddresses_delegatesToBothRepositoriesForCurrentTenant() {
        TenantContext.set(7L);
        when(studentRepository.suggestAddresses(7L, "kipé", 25)).thenReturn(List.of("Kipé, Conakry"));
        when(parentRepository.suggestAddresses(7L, "kipé", 25)).thenReturn(List.of());

        assertThat(service.suggestAddresses("Kipé")).containsExactly("Kipé, Conakry");
    }

    @Test
    void suggestFirstNames_blankOrTooShortQuery_returnsEmptyWithoutCallingRepositories() {
        assertThat(service.suggestFirstNames("")).isEmpty();
        assertThat(service.suggestFirstNames(null)).isEmpty();
        assertThat(service.suggestFirstNames("a")).isEmpty();
        verify(studentRepository, never()).suggestFirstNames(anyLong(), anyString(), anyInt());
        verify(parentRepository, never()).suggestFirstNames(anyLong(), anyString(), anyInt());
    }
}
