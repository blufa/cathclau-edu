package gn.lacaeducation.transport.application.port.out;

import gn.lacaeducation.transport.domain.TransportRoute;

import java.util.List;
import java.util.Optional;

public interface TransportRouteRepository {
    TransportRoute save(TransportRoute r);
    Optional<TransportRoute> findById(Long id);
    List<TransportRoute> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
