package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolCodeTest {

    @Test
    void buildsAcronymFromSignificantWords() {
        assertThat(SchoolCode.prefix("Groupe Scolaire La Cathédrale")).isEqualTo("GSC");
        assertThat(SchoolCode.prefix("École Primaire Les Anges")).isEqualTo("EPA");
    }

    @Test
    void singleWordKeepsFirstThreeLetters() {
        assertThat(SchoolCode.prefix("Renaissance")).isEqualTo("REN");
        assertThat(SchoolCode.prefix("La Renaissance")).isEqualTo("REN");
    }

    @Test
    void ignoresAccentsAndCaps() {
        assertThat(SchoolCode.prefix("collège évariste galois")).isEqualTo("CEG");
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
