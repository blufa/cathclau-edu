package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DjomySignatureTest {

    @Test
    void hmac_is_deterministic_64_hex_chars() {
        String a = DjomySignature.hmacHex("clientId", "secret");
        String b = DjomySignature.hmacHex("clientId", "secret");
        assertThat(a).isEqualTo(b).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void hmac_changes_with_secret_and_message() {
        String base = DjomySignature.hmacHex("clientId", "secret");
        assertThat(DjomySignature.hmacHex("clientId", "other")).isNotEqualTo(base);
        assertThat(DjomySignature.hmacHex("otherId", "secret")).isNotEqualTo(base);
    }

    @Test
    void apiKey_is_clientId_colon_hmac() {
        String key = DjomySignature.apiKey("cid", "sec");
        assertThat(key).isEqualTo("cid:" + DjomySignature.hmacHex("cid", "sec"));
    }

    @Test
    void webhook_signature_valid_when_prefix_and_hex_match() {
        String body = "{\"event\":\"payment.success\"}";
        String header = "v1:" + DjomySignature.hmacHex(body, "secret");
        assertThat(DjomySignature.verifyWebhook(body, header, "secret")).isTrue();
    }

    @Test
    void webhook_signature_rejected_on_bad_prefix_or_tamper() {
        String body = "{\"event\":\"payment.success\"}";
        String hex = DjomySignature.hmacHex(body, "secret");
        assertThat(DjomySignature.verifyWebhook(body, "v2:" + hex, "secret")).isFalse();
        assertThat(DjomySignature.verifyWebhook(body, hex, "secret")).isFalse();
        assertThat(DjomySignature.verifyWebhook(body + "x", "v1:" + hex, "secret")).isFalse();
        assertThat(DjomySignature.verifyWebhook(body, null, "secret")).isFalse();
    }
}
