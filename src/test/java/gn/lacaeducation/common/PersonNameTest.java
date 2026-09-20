package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonNameTest {

    @Test
    void capitalizeFirstName_titleCasesEachWord() {
        assertThat(PersonName.capitalizeFirstName("jean claude")).isEqualTo("Jean Claude");
        assertThat(PersonName.capitalizeFirstName("JEAN CLAUDE")).isEqualTo("Jean Claude");
        assertThat(PersonName.capitalizeFirstName("marie-line")).isEqualTo("Marie-Line");
        assertThat(PersonName.capitalizeFirstName("n'diaye")).isEqualTo("N'Diaye");
        assertThat(PersonName.capitalizeFirstName("  jean   claude  ")).isEqualTo("Jean Claude");
    }

    @Test
    void capitalizeFirstName_nullOrBlank_returnsAsIs() {
        assertThat(PersonName.capitalizeFirstName(null)).isNull();
        assertThat(PersonName.capitalizeFirstName("")).isEmpty();
        assertThat(PersonName.capitalizeFirstName("   ")).isEmpty();
    }

    @Test
    void upperCaseLastName_uppercasesAndCollapsesSpaces() {
        assertThat(PersonName.upperCaseLastName("camara")).isEqualTo("CAMARA");
        assertThat(PersonName.upperCaseLastName("  camara  diallo  ")).isEqualTo("CAMARA DIALLO");
        assertThat(PersonName.upperCaseLastName("Camara")).isEqualTo("CAMARA");
    }

    @Test
    void upperCaseLastName_null_returnsNull() {
        assertThat(PersonName.upperCaseLastName(null)).isNull();
    }
}
