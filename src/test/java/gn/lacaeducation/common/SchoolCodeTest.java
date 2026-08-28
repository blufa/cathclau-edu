package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolCodeTest {

    @Test
    void buildsAcronymFromSignificantWords() {
        // « Groupe » et « Scolaire » sont des désignations génériques d'établissement, ignorées pour
        // ne garder que ce qui distingue vraiment l'école (ex. « Al Taqwa » → « AT », pas « GSAT »).
        assertThat(SchoolCode.prefix("Groupe Scolaire Al Taqwa")).isEqualTo("AT");
        assertThat(SchoolCode.prefix("École Primaire Les Anges")).isEqualTo("PA");
    }

    @Test
    void singleWordKeepsFirstThreeLetters() {
        assertThat(SchoolCode.prefix("Renaissance")).isEqualTo("REN");
        assertThat(SchoolCode.prefix("La Renaissance")).isEqualTo("REN");
        // Un seul mot distinctif survit une fois « Groupe Scolaire » ignorés.
        assertThat(SchoolCode.prefix("Groupe Scolaire La Cathédrale")).isEqualTo("CAT");
    }

    @Test
    void ignoresAccentsAndCaps() {
        assertThat(SchoolCode.prefix("institut évariste galois")).isEqualTo("EG");
    }

    @Test
    void cappedAtSixLetters() {
        assertThat(SchoolCode.prefix("Alpha Beta Gamma Delta Epsilon Zeta Eta")).hasSize(6);
    }

    @Test
    void fallsBackToECWhenUnavailable() {
        assertThat(SchoolCode.prefix((String) null)).isEqualTo("EC");
        assertThat(SchoolCode.prefix("   ")).isEqualTo("EC");
        assertThat(SchoolCode.prefix("!!!")).isEqualTo("EC");
    }
}
