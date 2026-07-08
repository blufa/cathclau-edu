package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.application.port.out.GeneratedDocumentRepository;
import gn.lacaeducation.documents.domain.DocumentType;
import gn.lacaeducation.documents.domain.GeneratedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
@Component @RequiredArgsConstructor
class GeneratedDocumentPersistenceAdapter implements GeneratedDocumentRepository {
    private final GeneratedDocumentJpaRepository jpa;
    @Override public GeneratedDocument save(GeneratedDocument d) { return toDomain(jpa.save(toEntity(d))); }
    @Override public List<GeneratedDocument> findByTenantId(Long t) { return jpa.findByTenantIdOrderByGeneratedAtDesc(t).stream().map(this::toDomain).toList(); }
    @Override public long countByTenantIdAndType(Long t, DocumentType type) { return jpa.countByTenantIdAndDocumentType(t, type); }
    private GeneratedDocument toDomain(GeneratedDocumentJpaEntity e) {
        return GeneratedDocument.builder().id(e.getId()).tenantId(e.getTenantId()).templateId(e.getTemplateId())
                .documentType(e.getDocumentType()).reference(e.getReference()).title(e.getTitle())
                .subjectType(e.getSubjectType()).subjectId(e.getSubjectId()).generatedBy(e.getGeneratedBy())
                .generatedAt(e.getGeneratedAt()).build();
    }
    private GeneratedDocumentJpaEntity toEntity(GeneratedDocument d) {
        GeneratedDocumentJpaEntity e = new GeneratedDocumentJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setTemplateId(d.getTemplateId());
        e.setDocumentType(d.getDocumentType()); e.setReference(d.getReference()); e.setTitle(d.getTitle());
        e.setSubjectType(d.getSubjectType()); e.setSubjectId(d.getSubjectId()); e.setGeneratedBy(d.getGeneratedBy());
        e.setGeneratedAt(d.getGeneratedAt());
        return e;
    }
}
