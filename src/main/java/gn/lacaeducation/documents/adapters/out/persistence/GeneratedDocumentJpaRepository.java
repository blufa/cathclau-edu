package gn.lacaeducation.documents.adapters.out.persistence;
import gn.lacaeducation.documents.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface GeneratedDocumentJpaRepository extends JpaRepository<GeneratedDocumentJpaEntity, Long> {
    List<GeneratedDocumentJpaEntity> findByTenantIdOrderByGeneratedAtDesc(Long tenantId);
    long countByTenantIdAndDocumentType(Long tenantId, DocumentType type);
}
