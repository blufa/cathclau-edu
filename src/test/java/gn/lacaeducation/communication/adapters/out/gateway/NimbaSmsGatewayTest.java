package gn.lacaeducation.communication.adapters.out.gateway;

import gn.lacaeducation.communication.application.port.out.NotificationGateway.DeliveryResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Normalisation de numéro et dégradation en journalisation tant que Nimba n'est pas configuré. */
class NimbaSmsGatewayTest {

    @Test
    void normalizesAcceptedFormats() {
        assertThat(NimbaSmsGateway.normalizeNumber("623273737")).isEqualTo("623273737");           // local
        assertThat(NimbaSmsGateway.normalizeNumber("224623273737")).isEqualTo("224623273737");      // avec indicatif
        assertThat(NimbaSmsGateway.normalizeNumber("+224623273737")).isEqualTo("224623273737");     // avec +
        assertThat(NimbaSmsGateway.normalizeNumber("00224623273737")).isEqualTo("224623273737");    // préfixe 00
        assertThat(NimbaSmsGateway.normalizeNumber("+224 623 27 37 37")).isEqualTo("224623273737");  // espaces
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(NimbaSmsGateway.normalizeNumber(null)).isNull();
    }

    @Test
    void degradesToLoggingWhenNotConfigured() {
        NimbaSmsProperties props = new NimbaSmsProperties(false, null, null, null, null, null);
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, RestClient.builder());

        DeliveryResult result = gateway.send("623273737", "sujet", "contenu du message");

        assertThat(result.delivered()).isTrue();
        assertThat(result.detail()).contains("logged");
    }

    @Test
    void failsWhenDestinationIsBlank() {
        NimbaSmsProperties props = new NimbaSmsProperties(false, null, null, null, null, null);
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, RestClient.builder());

        DeliveryResult result = gateway.send("", "sujet", "contenu");

        assertThat(result.delivered()).isFalse();
    }

    @Test
    void failsWhenMessageIsEmpty() {
        NimbaSmsProperties props = new NimbaSmsProperties(false, null, null, null, null, null);
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, RestClient.builder());

        DeliveryResult result = gateway.send("623273737", "", "");

        assertThat(result.delivered()).isFalse();
    }

    @Test
    void fetchBalancesReturnsEmptyWhenNotConfigured() {
        NimbaSmsProperties props = new NimbaSmsProperties(false, null, null, null, null, null);
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, RestClient.builder());

        assertThat(gateway.fetchBalances()).isEmpty();
    }
}
