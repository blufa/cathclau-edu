package gn.lacaeducation.communication.application.port.in;

import gn.lacaeducation.communication.domain.SmsRetryEntry;

import java.util.List;

public interface SmsRetryQueueUseCase {
    record EnqueueCommand(Long tenantId, Long recipientId, String destination, String subject, String content, String lastError) {}

    SmsRetryEntry enqueue(EnqueueCommand cmd);
    List<SmsRetryEntry> findByTenant(Long tenantId);
    /** Relance manuelle depuis l'écran (indépendamment du job planifié) : remet l'entrée à zéro. */
    SmsRetryEntry replay(Long id);
}
