package gn.lacaeducation.communication.adapters.out.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Normalisation E.164 des numéros pour l'API SMS Orange (indicatif Guinée = 224). */
class OrangeSmsGatewayTest {

    @Test
    void normalizesLocalAndInternationalFormats() {
        assertThat(OrangeSmsGateway.toE164("620000000", "224")).isEqualTo("+224620000000");   // local
        assertThat(OrangeSmsGateway.toE164("00224620000000", "224")).isEqualTo("+224620000000"); // 00 + indicatif
        assertThat(OrangeSmsGateway.toE164("224620000000", "224")).isEqualTo("+224620000000");  // indicatif sans +
        assertThat(OrangeSmsGateway.toE164("+224620000000", "224")).isEqualTo("+224620000000"); // déjà E.164
        assertThat(OrangeSmsGateway.toE164("+224 620 00 00 00", "224")).isEqualTo("+224620000000"); // espaces
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(OrangeSmsGateway.toE164(null, "224")).isNull();
    }
}
