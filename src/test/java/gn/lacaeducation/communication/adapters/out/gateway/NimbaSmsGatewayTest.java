package gn.lacaeducation.communication.adapters.out.gateway;

import gn.lacaeducation.communication.application.port.out.NotificationGateway.DeliveryResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** Normalisation de numéro et dégradation en journalisation tant que Nimba n'est pas configuré. */
class NimbaSmsGatewayTest {

    @Test
    void normalizesToStrictE164WithPlus() {
        // Un SMS a déjà été accepté (HTTP 201) par Nimba mais jamais livré avec un numéro sans "+" —
        // on force désormais systématiquement la forme +224XXXXXXXXX, la plus stricte des 3 acceptées.
        assertThat(NimbaSmsGateway.normalizeNumber("623273737", "224")).isEqualTo("+224623273737");           // local
        assertThat(NimbaSmsGateway.normalizeNumber("224623273737", "224")).isEqualTo("+224623273737");        // avec indicatif
        assertThat(NimbaSmsGateway.normalizeNumber("+224623273737", "224")).isEqualTo("+224623273737");       // avec +
        assertThat(NimbaSmsGateway.normalizeNumber("00224623273737", "224")).isEqualTo("+224623273737");      // préfixe 00
        assertThat(NimbaSmsGateway.normalizeNumber("+224 623 27 37 37", "224")).isEqualTo("+224623273737");   // espaces
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(NimbaSmsGateway.normalizeNumber(null, "224")).isNull();
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

    /** Preuve concrète (pas seulement une relecture du code) du corps EXACTEMENT envoyé sur le fil pour
     * un envoi configuré : exactement 3 champs (sender_name, to, message), aucun champ en trop — un
     * garde-fou de non-régression après le double correctif (retrait de `channel`, puis sérialisation
     * manuelle pour éliminer toute incertitude côté Spring/Jackson auto-configuré). */
    @Test
    void configuredSend_postsExactBodyWithOnlyThreeFields() {
        NimbaSmsProperties props = new NimbaSmsProperties(
                true, "svc-id", "secret-token", "http://nimba.test", "JECCAMTECH", "224");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, builder);

        server.expect(requestTo("http://nimba.test/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + java.util.Base64.getEncoder()
                        .encodeToString("svc-id:secret-token".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender_name").value("JECCAMTECH"))
                .andExpect(jsonPath("$.to[0]").value("+224623273737"))
                .andExpect(jsonPath("$.message").value("Bonjour"))
                .andExpect(jsonPath("$.channel").doesNotExist())
                .andRespond(withSuccess("{\"messageid\":\"MSG-1\"}", MediaType.APPLICATION_JSON));

        DeliveryResult result = gateway.send("623273737", "sujet", "Bonjour");

        assertThat(result.delivered()).isTrue();
        assertThat(result.detail()).contains("MSG-1");
        server.verify();
    }

    @Test
    void configuredSend_400Response_isReportedAsFailureWithNimbaMessage() {
        NimbaSmsProperties props = new NimbaSmsProperties(
                true, "svc-id", "secret-token", "http://nimba.test", "JECCAMTECH", "224");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, builder);

        server.expect(requestTo("http://nimba.test/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body("{\"sender_name\":[\"Ce champ est obligatoire.\"]}")
                        .contentType(MediaType.APPLICATION_JSON));

        DeliveryResult result = gateway.send("623273737", "sujet", "Bonjour");

        assertThat(result.delivered()).isFalse();
        server.verify();
    }

    /**
     * Régression incident de production : un 4xx (numéro structurellement invalide, champ manquant...)
     * est un échec DÉFINITIF — jamais transitoire — donc {@code retryable()} doit être faux, pour que
     * {@code SmsRetryDispatchScheduler} abandonne au lieu de retenter indéfiniment toutes les 30 min
     * (des dizaines d'entrées de ce type ont fini par saturer CPU/connexions DB en production).
     */
    @Test
    void configuredSend_400Response_isNotRetryable() {
        NimbaSmsProperties props = new NimbaSmsProperties(
                true, "svc-id", "secret-token", "http://nimba.test", "JECCAMTECH", "224");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, builder);

        server.expect(requestTo("http://nimba.test/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body("{\"to\":\"Aucun numéro valide fourni.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        DeliveryResult result = gateway.send("620", "sujet", "Bonjour");

        assertThat(result.delivered()).isFalse();
        assertThat(result.retryable()).isFalse();
        server.verify();
    }

    /** Une panne serveur (5xx) ou réseau reste retryable comme avant — seul un 4xx (erreur définitive
     * sur CE message précis) ne l'est pas. */
    @Test
    void configuredSend_500Response_staysRetryable() {
        NimbaSmsProperties props = new NimbaSmsProperties(
                true, "svc-id", "secret-token", "http://nimba.test", "JECCAMTECH", "224");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NimbaSmsGateway gateway = new NimbaSmsGateway(props, builder);

        server.expect(requestTo("http://nimba.test/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("panne serveur"));

        DeliveryResult result = gateway.send("623273737", "sujet", "Bonjour");

        assertThat(result.delivered()).isFalse();
        assertThat(result.retryable()).isTrue();
        server.verify();
    }
}
