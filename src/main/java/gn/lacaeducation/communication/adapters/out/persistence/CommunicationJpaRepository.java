package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface CommunicationJpaRepository extends JpaRepository<CommunicationJpaEntity, Long> {
    List<CommunicationJpaEntity> findByTenantId(Long tenantId);
    List<CommunicationJpaEntity> findBySenderId(Long senderId);
    List<CommunicationJpaEntity> findByTenantIdAndChannel(Long tenantId, CommunicationChannel channel);
    List<CommunicationJpaEntity> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
}
