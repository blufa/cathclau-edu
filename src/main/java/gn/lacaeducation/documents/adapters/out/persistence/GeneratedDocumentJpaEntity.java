package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.domain.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "generated_documents")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class GeneratedDocumentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "template_id") private Long templateId;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 40) private DocumentType documentType;
    @Column(nullable = false, length = 60) private String reference;
    @Column(length = 255) private String title;
    @Column(name = "subject_type", length = 24) private String subjectType;
    @Column(name = "subject_id") private Long subjectId;
    @Column(name = "generated_by") private Long generatedBy;
    @Column(name = "generated_at") private LocalDateTime generatedAt;
}
