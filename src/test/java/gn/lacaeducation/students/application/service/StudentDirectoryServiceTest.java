package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.out.StudentDirectoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDirectoryServiceTest {

    @Mock StudentDirectoryPort directory;
    @InjectMocks StudentDirectoryService service;

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
}
