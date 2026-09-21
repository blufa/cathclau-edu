package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculeFormatterTest {

    @Test
    void nullFormatAndDigits_useHistoricalDefault() {
        assertThat(MatriculeFormatter.generate(null, null, "AT", 1)).isEqualTo("AT00001");
    }

    @Test
    void blankFormat_usesHistoricalDefault() {
        assertThat(MatriculeFormatter.generate("  ", null, "AT", 42)).isEqualTo("AT00042");
    }

    @Test
    void customFormat_replacesAllTokens() {
        String year = String.valueOf(Year.now().getValue());
        assertThat(MatriculeFormatter.generate("{PREFIXE}-{ANNEE}-{NUM}", 4, "AT", 7))
                .isEqualTo("AT-" + year + "-0007");
    }

    @Test
    void customDigits_padsSequenceAccordingly() {
        assertThat(MatriculeFormatter.generate("{PREFIXE}{NUM}", 3, "AT", 5)).isEqualTo("AT005");
        assertThat(MatriculeFormatter.generate("{PREFIXE}{NUM}", 8, "AT", 5)).isEqualTo("AT00000005");
    }

    @Test
    void digitsBeyondMax_areClamped() {
        assertThat(MatriculeFormatter.generate("{NUM}", 50, null, 1)).hasSize(10);
    }

    @Test
    void nullPrefix_treatedAsEmpty() {
        assertThat(MatriculeFormatter.generate("{PREFIXE}{NUM}", 3, null, 1)).isEqualTo("001");
    }

    @Test
    void tokensCanBeReorderedOrOmittedFreely() {
        assertThat(MatriculeFormatter.generate("ELV{NUM}", 5, "AT", 1)).isEqualTo("ELV00001");
        assertThat(MatriculeFormatter.generate("{NUM}-{PREFIXE}", 5, "AT", 1)).isEqualTo("00001-AT");
    }
}
