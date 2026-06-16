package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Drives DjomyClient against a mocked HTTP server (MockRestServiceServer) — never the real sandbox.
 * Verifies the two-step auth (X-API-KEY → Bearer), the dual-header requirement, the redirectUrl
 * extraction and the verify_payment currency mapping / receivedAmount preference.
 */
class DjomyClientTest {

    private record Fixture(DjomyClient client, MockRestServiceServer server) {}

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DjomyProperties props = new DjomyProperties("cid", "secret", "http://djomy.test", "GNF", "http://return");
        return new Fixture(new DjomyClient(props, builder), server);
    }

    @Test
    void create_payment_gateway_authenticates_and_returns_redirect_url() {
        Fixture f = fixture();
        f.server().expect(requestTo("http://djomy.test/v1/auth"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", DjomySignature.apiKey("cid", "secret")))
                .andRespond(withSuccess("{\"token\":\"tok-123\",\"expiresIn\":3600}", MediaType.APPLICATION_JSON));
        f.server().expect(requestTo("http://djomy.test/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok-123"))
                .andExpect(header("X-API-KEY", DjomySignature.apiKey("cid", "secret")))
                .andRespond(withSuccess("{\"transactionId\":\"TX-1\",\"redirectUrl\":\"http://portal/pay/TX-1\"}", MediaType.APPLICATION_JSON));

        DjomyClient.GatewayResult result = f.client()
                .createPaymentGateway("620000000", new BigDecimal("5000"), "PAY-1", null);

        assertThat(result.transactionId()).isEqualTo("TX-1");
        assertThat(result.redirectUrl()).isEqualTo("http://portal/pay/TX-1");
        f.server().verify();
    }

    @Test
    void create_payment_gateway_requires_payer_number() {
        Fixture f = fixture();
        assertThatThrownBy(() -> f.client().createPaymentGateway("  ", BigDecimal.TEN, "PAY-2", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_payment_maps_currency_and_prefers_received_amount() {
        Fixture f = fixture();
        f.server().expect(requestTo("http://djomy.test/v1/auth"))
                .andRespond(withSuccess("{\"token\":\"tok\",\"expiresIn\":3600}", MediaType.APPLICATION_JSON));
        f.server().expect(requestTo("http://djomy.test/v1/payments/TX-9"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess(
                        "{\"status\":\"SUCCESS\",\"paidAmount\":5000,\"receivedAmount\":4900,\"currency\":\"FCFA\"}",
                        MediaType.APPLICATION_JSON));

        DjomyClient.Verification v = f.client().verifyPayment("TX-9");

        assertThat(v.isSuccess()).isTrue();
        assertThat(v.receivedAmount()).isEqualByComparingTo("4900");
        assertThat(v.currency()).isEqualTo("XOF");
        f.server().verify();
    }
}
