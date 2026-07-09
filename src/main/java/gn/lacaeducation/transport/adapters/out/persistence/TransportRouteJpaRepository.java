package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TransportRouteJpaRepository extends JpaRepository<TransportRouteJpaEntity, Long> {
    List<TransportRouteJpaEntity> findByTenantId(Long tenantId);
}
