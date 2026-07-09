package gn.lacaeducation.transport.application.port.out;

import gn.lacaeducation.transport.domain.TransportStop;

import java.util.List;
import java.util.Optional;

public interface TransportStopRepository {
    TransportStop save(TransportStop s);
    Optional<TransportStop> findById(Long id);
    List<TransportStop> findByRouteIdOrderBySequenceAsc(Long routeId);
    void deleteById(Long id);
}
