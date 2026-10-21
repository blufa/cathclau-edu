package gn.lacaeducation.common;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Uploads user files (student photos, documents, logos, justificatifs) to Cloudinary via the signed
 * REST upload API ({@code /auto/upload}) and returns the {@code secure_url}. No local storage.
 */
@Slf4j
@Service
public class MediaUploadService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public MediaUploadService(@Value("${app.cloudinary.cloud-name:}") String cloudName,
                              @Value("${app.cloudinary.api-key:}") String apiKey,
                              @Value("${app.cloudinary.api-secret:}") String apiSecret,
                              ObjectMapper objectMapper) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.objectMapper = objectMapper;
    }

    /** Uploads the file under the given folder and returns its secure URL. */
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Fichier vide");
        }
        // §11.3 cahier des charges : contrôler le type de fichier côté serveur pour un upload de photo
        // (élève/enseignant/personnel, tous dans un dossier "*/photos" par convention frontend) — la
        // taille globale est déjà plafonnée par spring.servlet.multipart.max-file-size (15 Mo).
        if (folder != null && folder.endsWith("/photos")) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessRuleException("Le fichier doit être une image (JPEG, PNG…) pour une photo de profil.");
            }
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Lecture du fichier impossible");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        return uploadBytes(content, filename, folder);
    }

    /** Uploads raw bytes (e.g. a generated PDF) under the given folder and returns its secure URL. */
    public String uploadBytes(byte[] content, String filename, String folder) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new BusinessRuleException("Cloudinary n'est pas configuré (app.cloudinary.*)");
        }
        if (content == null || content.length == 0) {
            throw new BusinessRuleException("Fichier vide");
        }
        long timestamp = System.currentTimeMillis() / 1000L;
        String safeFolder = (folder == null || folder.isBlank()) ? "uploads" : folder.trim();
        String toSign = "folder=" + safeFolder + "&timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);
        String name = filename != null ? filename : "file";

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return name; }
        });
        form.add("api_key", apiKey);
        form.add("timestamp", String.valueOf(timestamp));
        form.add("folder", safeFolder);
        form.add("signature", signature);

        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
        String body = restClient.post().uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(String.class);
        JsonNode node = objectMapper.readTree(body);
        String secureUrl = node.path("secure_url").asText(null);
        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary upload returned no secure_url: " + body);
        }
        log.info("Uploaded {} to Cloudinary folder {}: {}", name, safeFolder, secureUrl);
        return secureUrl;
    }

    /**
     * Uploads a generated document (PDF…) as a Cloudinary <b>raw</b> resource so the bytes are served
     * unchanged — unlike {@code auto/upload}, which classifies a PDF as an image (rasterisation /
     * "PDF & ZIP delivery" disabled by default), giving a blank preview. Returns the secure URL.
     */
    public String uploadDocument(byte[] content, String filename, String folder) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new BusinessRuleException("Cloudinary n'est pas configuré (app.cloudinary.*)");
        }
        if (content == null || content.length == 0) {
            throw new BusinessRuleException("Fichier vide");
        }
        long timestamp = System.currentTimeMillis() / 1000L;
        String name = (filename != null && !filename.isBlank()) ? filename : "document.pdf";
        String publicId = ((folder == null || folder.isBlank()) ? "" : folder.trim() + "/") + name;
        String toSign = "public_id=" + publicId + "&timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override public String getFilename() { return name; }
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
        JsonNode node = objectMapper.readTree(body);
        String secureUrl = node.path("secure_url").asText(null);
        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary raw upload returned no secure_url: " + body);
        }
        log.info("Uploaded document {} to Cloudinary (raw): {}", publicId, secureUrl);
        return secureUrl;
    }

    static String sha1Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
