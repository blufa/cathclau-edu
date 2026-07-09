package gn.lacaeducation.documents.application.port.out;
import gn.lacaeducation.documents.domain.DocumentTemplate;
import gn.lacaeducation.documents.domain.DocumentType;
import java.util.List;
import java.util.Optional;
public interface DocumentTemplateRepository {
    DocumentTemplate save(DocumentTemplate t);
    Optional<DocumentTemplate> findById(Long id);
    List<DocumentTemplate> findByTenantId(Long tenantId);
    Optional<DocumentTemplate> findDefault(Long tenantId, DocumentType type);
    List<DocumentTemplate> findByTenantIdAndType(Long tenantId, DocumentType type);
    void deleteById(Long id);
}
