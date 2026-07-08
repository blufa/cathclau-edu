package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface DocumentTemplateJpaRepository extends JpaRepository<DocumentTemplateJpaEntity, Long> {
    List<DocumentTemplateJpaEntity> findByTenantId(Long tenantId);
    List<DocumentTemplateJpaEntity> findByTenantIdAndDocumentType(Long tenantId, DocumentType type);
    Optional<DocumentTemplateJpaEntity> findFirstByTenantIdAndDocumentTypeAndIsDefaultTrue(Long tenantId, DocumentType type);
}
