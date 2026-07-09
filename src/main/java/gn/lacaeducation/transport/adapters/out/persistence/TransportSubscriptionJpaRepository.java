package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TransportSubscriptionJpaRepository extends JpaRepository<TransportSubscriptionJpaEntity, Long> {
    List<TransportSubscriptionJpaEntity> findByTenantId(Long tenantId);
    List<TransportSubscriptionJpaEntity> findByRouteId(Long routeId);
    List<TransportSubscriptionJpaEntity> findByStudentId(Long studentId);
}
