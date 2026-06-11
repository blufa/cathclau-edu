package gn.lacaeducation.communication.application.port.out;
import gn.lacaeducation.communication.domain.*;
import java.util.List;
import java.util.Optional;
public interface CommunicationRepository {
    Communication save(Communication communication);
    Optional<Communication> findById(Long id);
    List<Communication> findByTenantId(Long tenantId);
    List<Communication> findBySenderId(Long senderId);
    List<Communication> findByTenantIdAndChannel(Long tenantId, CommunicationChannel channel);
    List<Communication> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
    void deleteById(Long id);
}
