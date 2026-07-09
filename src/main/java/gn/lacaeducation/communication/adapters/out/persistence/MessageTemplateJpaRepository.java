package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.CommunicationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface MessageTemplateJpaRepository extends JpaRepository<MessageTemplateJpaEntity, Long> {
    List<MessageTemplateJpaEntity> findByTenantId(Long tenantId);
    Optional<MessageTemplateJpaEntity> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
}
