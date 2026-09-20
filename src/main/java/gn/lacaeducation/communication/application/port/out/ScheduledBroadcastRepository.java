package gn.lacaeducation.communication.application.port.out;

import gn.lacaeducation.communication.domain.ScheduledBroadcast;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledBroadcastRepository {
    ScheduledBroadcast save(ScheduledBroadcast broadcast);

    Optional<ScheduledBroadcast> findById(Long id);

    List<ScheduledBroadcast> findByTenantId(Long tenantId);

    /** Diffusions PENDING dont l'heure de déclenchement est passée — à traiter par le job planifié. */
    List<ScheduledBroadcast> findDuePending(Long tenantId, LocalDateTime now);
}
