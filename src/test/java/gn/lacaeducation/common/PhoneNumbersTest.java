package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

    @Test
    void localInput_includesInternationalVariant() {
        assertThat(PhoneNumbers.candidates("620999001")).contains("620999001", "+224620999001");
    }

    @Test
    void internationalInput_includesLocalVariant() {
        // Régression (§retour comptable) : avant correctif, cette direction ne générait QUE la forme
        // internationale elle-même, jamais la forme locale — un parent/utilisateur enregistré en local
        // puis ressaisi en international (ou l'inverse) n'était pas détecté comme un doublon.
        assertThat(PhoneNumbers.candidates("+224620999001")).contains("+224620999001", "620999001");
    }

    @Test
    void doubleZeroInternationalPrefix_includesLocalAndPlusVariant() {
        assertThat(PhoneNumbers.candidates("00224620999001")).contains("620999001", "+224620999001");
    }

    @Test
    void spacesInInput_areStrippedConsistently() {
        assertThat(PhoneNumbers.candidates("+224 620 999 001")).contains("620999001", "+224620999001");
    }

    @Test
    void localAndInternationalInputs_produceOverlappingCandidateSets() {
        // C'est cette propriété précise qui garantit la détection de doublon quel que soit le format
        // sous lequel chacun des deux numéros a été saisi (voir ParentService#create, UserService).
        var fromLocal = PhoneNumbers.candidates("620999001");
        var fromIntl = PhoneNumbers.candidates("+224620999001");
        assertThat(fromLocal).containsAnyElementsOf(fromIntl);
    }

    @Test
    void nullOrNonPhoneInput_returnsEmptyList() {
        assertThat(PhoneNumbers.candidates(null)).isEmpty();
        assertThat(PhoneNumbers.candidates("abc")).isEmpty();
        assertThat(PhoneNumbers.candidates("1234")).isEmpty(); // trop court
    }
}
