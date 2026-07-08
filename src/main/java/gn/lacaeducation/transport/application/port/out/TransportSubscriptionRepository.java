package gn.lacaeducation.transport.application.port.out;

import gn.lacaeducation.transport.domain.TransportSubscription;

import java.util.List;
import java.util.Optional;

public interface TransportSubscriptionRepository {
    TransportSubscription save(TransportSubscription s);
    Optional<TransportSubscription> findById(Long id);
    List<TransportSubscription> findByTenantId(Long tenantId);
    List<TransportSubscription> findByRouteId(Long routeId);
    List<TransportSubscription> findByStudentId(Long studentId);
    void deleteById(Long id);
}
