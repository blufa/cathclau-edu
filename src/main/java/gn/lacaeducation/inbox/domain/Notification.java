package gn.lacaeducation.inbox.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification in-app (§13) destinée à un utilisateur (parent en premier). Consultable dans la cloche
 * de notifications ; peut être marquée comme lue. Générée automatiquement sur événements métier.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    private Long id;
    private Long tenantId;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    /** Lien in-app vers la ressource concernée (bulletin, facture…). */
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
}
