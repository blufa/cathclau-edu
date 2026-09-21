package gn.lacaeducation.communication.application.port.in;

import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.ScheduledBroadcast;

import java.time.LocalDateTime;
import java.util.List;

/** Programmation d'une diffusion de masse (§11.1 bonus) : déclenchée automatiquement à {@code sendAt}
 * par le job planifié, sans intervention manuelle. */
public interface ScheduledBroadcastUseCase {
    record ScheduleCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                           BroadcastAudience audience, Long classId, Long studentId,
                           String subject, String content, CommunicationReason reason,
                           LocalDateTime sendAt, Long schoolEventId) {}

    ScheduledBroadcast schedule(ScheduleCommand command);

    List<ScheduledBroadcast> findByTenant(Long tenantId);

    /** Annule une diffusion tant qu'elle n'est pas encore partie (PENDING uniquement). */
    void cancel(Long id);

    /** Déclenche toutes les diffusions PENDING dues pour ce tenant — appelé par le job planifié. */
    void processDueForTenant(Long tenantId, LocalDateTime now);
}
