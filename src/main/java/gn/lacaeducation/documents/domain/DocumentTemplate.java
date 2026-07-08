package gn.lacaeducation.documents.domain;
import lombok.*;
import java.time.LocalDateTime;
/** Modèle de document personnalisable (branding, format, en-têtes/pieds, variables, numérotation). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentTemplate {
    private Long id;
    private Long tenantId;
    private DocumentCategory category;
    private DocumentType documentType;
    private String name;
    private DocumentStyle style;
    private String headerHtml;
    private String bodyHtml;
    private String footerHtml;
    private String primaryColor;
    private String fontFamily;
    private String logoUrl;
    private String signatureUrl;
    private String stampUrl;
    private String watermarkText;
    private PageFormat pageFormat;
    private Integer marginTop;
    private Integer marginRight;
    private Integer marginBottom;
    private Integer marginLeft;
    private String numberingPrefix;
    private Integer version;
    private Boolean isDefault;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
