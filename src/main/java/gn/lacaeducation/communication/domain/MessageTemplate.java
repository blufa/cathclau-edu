package gn.lacaeducation.communication.domain;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Per-tenant message template (§11.2 BONUS). The {@code body}/{@code subject} carry
 * {@code {{variable}}} placeholders that are substituted at send time.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageTemplate {
    private Long id;
    private Long tenantId;
    private CommunicationReason reason;
    private CommunicationChannel channel;
    private String subject;
    private String body;
    private LocalDateTime createdAt;

    /** Replaces every {@code {{key}}} occurrence in {@code text} with the matching variable value. */
    public static String render(String text, Map<String, String> variables) {
        if (text == null) return null;
        String result = text;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return result;
    }
}
