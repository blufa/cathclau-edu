package gn.lacaeducation.studentcards.domain;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardTemplate {
    private Long id;
    private Long tenantId;
    private String name;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String backgroundUrl;
    private CardOrientation orientation;
    private String rectoConfig;   // JSON (champs affichés)
    private String versoConfig;   // JSON (règlement, contacts)
    private Boolean isDefault;
    private Boolean active;
}
