package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Djomy signing primitives (P9/P10). All signatures are HMAC-SHA256 rendered as lowercase hex:
 *  - auth / X-API-KEY: HMAC(clientId, clientSecret)
 *  - webhook: HMAC(rawBody, clientSecret)
 */
public final class DjomySignature {

    private DjomySignature() {}

    /** HMAC-SHA256 of {@code message} keyed by {@code secret}, lowercase hex. */
    public static String hmacHex(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute Djomy HMAC signature", e);
        }
    }

    /** The {@code X-API-KEY} header value: "{clientId}:{HMAC(clientId, clientSecret)}". */
    public static String apiKey(String clientId, String clientSecret) {
        return clientId + ":" + hmacHex(clientId, clientSecret);
    }

    /**
     * Verifies a Djomy webhook signature header of the form "v1:&lt;hex&gt;" against the raw body,
     * in constant time. Returns false if the prefix is not "v1" or the hex does not match.
     */
    public static boolean verifyWebhook(String rawBody, String signatureHeader, String clientSecret) {
        if (signatureHeader == null || !signatureHeader.startsWith("v1:")) return false;
        String provided = signatureHeader.substring(3);
        String expected = hmacHex(rawBody, clientSecret);
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
