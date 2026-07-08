package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.application.port.out.DocumentTemplateRepository;
import gn.lacaeducation.documents.domain.DocumentTemplate;
import gn.lacaeducation.documents.domain.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class DocumentTemplatePersistenceAdapter implements DocumentTemplateRepository {
    private final DocumentTemplateJpaRepository jpa;
    @Override public DocumentTemplate save(DocumentTemplate t) { return toDomain(jpa.save(toEntity(t))); }
    @Override public Optional<DocumentTemplate> findById(Long id) { return jpa.findById(id).map(this::toDomain); }
    @Override public List<DocumentTemplate> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(this::toDomain).toList(); }
    @Override public Optional<DocumentTemplate> findDefault(Long t, DocumentType type) { return jpa.findFirstByTenantIdAndDocumentTypeAndIsDefaultTrue(t, type).map(this::toDomain); }
    @Override public List<DocumentTemplate> findByTenantIdAndType(Long t, DocumentType type) { return jpa.findByTenantIdAndDocumentType(t, type).stream().map(this::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    private DocumentTemplate toDomain(DocumentTemplateJpaEntity e) {
        return DocumentTemplate.builder().id(e.getId()).tenantId(e.getTenantId()).category(e.getCategory())
                .documentType(e.getDocumentType()).name(e.getName()).style(e.getStyle())
                .headerHtml(e.getHeaderHtml()).bodyHtml(e.getBodyHtml()).footerHtml(e.getFooterHtml())
                .primaryColor(e.getPrimaryColor()).fontFamily(e.getFontFamily()).logoUrl(e.getLogoUrl())
                .signatureUrl(e.getSignatureUrl()).stampUrl(e.getStampUrl()).watermarkText(e.getWatermarkText())
                .pageFormat(e.getPageFormat()).marginTop(e.getMarginTop()).marginRight(e.getMarginRight())
                .marginBottom(e.getMarginBottom()).marginLeft(e.getMarginLeft()).numberingPrefix(e.getNumberingPrefix())
                .version(e.getVersion()).isDefault(e.getIsDefault()).active(e.getActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }
    private DocumentTemplateJpaEntity toEntity(DocumentTemplate t) {
        DocumentTemplateJpaEntity e = new DocumentTemplateJpaEntity();
        e.setId(t.getId()); e.setTenantId(t.getTenantId()); e.setCategory(t.getCategory());
        e.setDocumentType(t.getDocumentType()); e.setName(t.getName()); e.setStyle(t.getStyle());
        e.setHeaderHtml(t.getHeaderHtml()); e.setBodyHtml(t.getBodyHtml()); e.setFooterHtml(t.getFooterHtml());
        e.setPrimaryColor(t.getPrimaryColor()); e.setFontFamily(t.getFontFamily()); e.setLogoUrl(t.getLogoUrl());
        e.setSignatureUrl(t.getSignatureUrl()); e.setStampUrl(t.getStampUrl()); e.setWatermarkText(t.getWatermarkText());
        e.setPageFormat(t.getPageFormat()); e.setMarginTop(t.getMarginTop()); e.setMarginRight(t.getMarginRight());
        e.setMarginBottom(t.getMarginBottom()); e.setMarginLeft(t.getMarginLeft()); e.setNumberingPrefix(t.getNumberingPrefix());
        e.setVersion(t.getVersion()); e.setIsDefault(t.getIsDefault()); e.setActive(t.getActive());
        e.setCreatedAt(t.getCreatedAt()); e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }
}
