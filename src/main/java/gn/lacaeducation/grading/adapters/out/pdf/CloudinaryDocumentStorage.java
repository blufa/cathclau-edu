package gn.lacaeducation.grading.adapters.out.pdf;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gn.lacaeducation.grading.application.port.out.DocumentStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Stores generated documents (PDFs) on Cloudinary instead of the local filesystem.
 *
 * <p>Primary {@link DocumentStoragePort}: uploads bytes through Cloudinary's signed REST upload API
 * (no extra SDK dependency) and returns the {@code secure_url}. Credentials are supplied via
 * configuration ({@code app.cloudinary.cloud-name}, {@code app.cloudinary.api-key},
 * {@code app.cloudinary.api-secret}); fill them in once available.</p>
 */
@Slf4j
@Primary
@Component
class CloudinaryDocumentStorage implements DocumentStoragePort {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    CloudinaryDocumentStorage(@Value("${app.cloudinary.cloud-name:}") String cloudName,
                              @Value("${app.cloudinary.api-key:}") String apiKey,
                              @Value("${app.cloudinary.api-secret:}") String apiSecret,
                              ObjectMapper objectMapper) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.objectMapper = objectMapper;
    }

    @Override
    public String store(String filename, byte[] content) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException("Cloudinary credentials are not configured "
                    + "(set app.cloudinary.cloud-name / api-key / api-secret)");
        }
        long timestamp = System.currentTimeMillis() / 1000L;
        String publicId = filename;
        // Cloudinary signature: SHA-1 of the alphabetically-sorted signed params + api_secret.
        String toSign = "public_id=" + publicId + "&timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return filename; }
        });
        form.add("api_key", apiKey);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("public_id", publicId);
        form.add("signature", signature);

        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/raw/upload";
        String body = restClient.post().uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = objectMapper.readTree(body);
            String secureUrl = node.path("secure_url").asText(null);
            if (secureUrl == null) {
                throw new IllegalStateException("Cloudinary upload returned no secure_url: " + body);
            }
            log.info("Stored document {} on Cloudinary: {}", filename, secureUrl);
            return secureUrl;
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to parse Cloudinary response", e);
        }
    }

    @Override
    public byte[] read(String location) {
        return restClient.get().uri(location).retrieve().body(byte[].class);
    }

    static String sha1Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
