package gn.lacaeducation.documents.domain;
import lombok.*;
import java.time.LocalDateTime;
/** Trace d'une génération de document (historique + numérotation). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GeneratedDocument {
    private Long id;
    private Long tenantId;
    private Long templateId;
    private DocumentType documentType;
    private String reference;
    private String title;
    private String subjectType;
    private Long subjectId;
    private Long generatedBy;
    private LocalDateTime generatedAt;
}
