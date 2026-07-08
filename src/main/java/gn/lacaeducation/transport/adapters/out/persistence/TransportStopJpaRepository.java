package gn.lacaeducation.transport.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TransportStopJpaRepository extends JpaRepository<TransportStopJpaEntity, Long> {
    List<TransportStopJpaEntity> findByRouteIdOrderBySequenceAsc(Long routeId);
}
