package gn.lacaeducation.documents.application.port.out;
import gn.lacaeducation.documents.domain.DocumentType;
import gn.lacaeducation.documents.domain.GeneratedDocument;
import java.util.List;
public interface GeneratedDocumentRepository {
    GeneratedDocument save(GeneratedDocument d);
    List<GeneratedDocument> findByTenantId(Long tenantId);
    long countByTenantIdAndType(Long tenantId, DocumentType type);
}
