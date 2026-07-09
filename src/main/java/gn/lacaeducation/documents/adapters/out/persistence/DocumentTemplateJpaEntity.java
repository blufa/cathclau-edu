package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.domain.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "document_templates")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class DocumentTemplateJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private DocumentCategory category;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 40) private DocumentType documentType;
    @Column(nullable = false, length = 160) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DocumentStyle style;
    @Column(name = "header_html", columnDefinition = "text") private String headerHtml;
    @Column(name = "body_html", columnDefinition = "text") private String bodyHtml;
    @Column(name = "footer_html", columnDefinition = "text") private String footerHtml;
    @Column(name = "primary_color", length = 16) private String primaryColor;
    @Column(name = "font_family", length = 80) private String fontFamily;
    @Column(name = "logo_url", length = 1024) private String logoUrl;
    @Column(name = "signature_url", length = 1024) private String signatureUrl;
    @Column(name = "stamp_url", length = 1024) private String stampUrl;
    @Column(name = "watermark_text", length = 120) private String watermarkText;
    @Enumerated(EnumType.STRING) @Column(name = "page_format", nullable = false, length = 8) private PageFormat pageFormat;
    @Column(name = "margin_top", nullable = false) private Integer marginTop;
    @Column(name = "margin_right", nullable = false) private Integer marginRight;
    @Column(name = "margin_bottom", nullable = false) private Integer marginBottom;
    @Column(name = "margin_left", nullable = false) private Integer marginLeft;
    @Column(name = "numbering_prefix", length = 40) private String numberingPrefix;
    @Column(nullable = false) private Integer version;
    @Column(name = "is_default", nullable = false) private Boolean isDefault;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}
