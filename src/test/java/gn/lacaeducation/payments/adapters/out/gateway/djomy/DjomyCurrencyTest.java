package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DjomyCurrencyTest {

    @Test
    void maps_non_iso_labels_to_iso() {
        assertThat(DjomyCurrency.toIso("GNF")).isEqualTo("GNF");
        assertThat(DjomyCurrency.toIso("FCFA")).isEqualTo("XOF");
        assertThat(DjomyCurrency.toIso("Dollar")).isEqualTo("USD");
        assertThat(DjomyCurrency.toIso("Leones")).isEqualTo("SLE");
    }

    @Test
    void is_case_insensitive_and_trims() {
        assertThat(DjomyCurrency.toIso("  fcfa ")).isEqualTo("XOF");
    }

    @Test
    void unknown_or_null_is_returned_unchanged() {
        assertThat(DjomyCurrency.toIso("EUR")).isEqualTo("EUR");
        assertThat(DjomyCurrency.toIso(null)).isNull();
    }
}
