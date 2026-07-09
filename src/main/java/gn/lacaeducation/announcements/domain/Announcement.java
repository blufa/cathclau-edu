package gn.lacaeducation.announcements.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Annonce de l'établissement (§8). Diffusée à toute l'école (GENERAL), à une classe (CLASS) ou à un
 * élève précis (STUDENT). L'auteur est dénormalisé (nom) pour l'affichage sans dépendance à iam.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Announcement {
    private Long id;
    private Long tenantId;
    private String title;
    private String content;
    private AnnouncementScope scope;
    /** Requis si scope = CLASS. */
    private Long classId;
    /** Requis si scope = STUDENT. */
    private Long studentId;
    private Long authorId;
    private String authorName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
