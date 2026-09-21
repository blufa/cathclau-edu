package gn.lacaeducation.communication.application.port.out;

import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmsRetryQueueRepository {
    SmsRetryEntry save(SmsRetryEntry e);
    Optional<SmsRetryEntry> findById(Long id);
    List<SmsRetryEntry> findByTenantId(Long tenantId);
    /** Entrées prêtes à être (re)tentées — tous tenants confondus, pour le job planifié. */
    List<SmsRetryEntry> findByStatusAndNextAttemptAtBefore(SmsRetryStatus status, LocalDateTime before);
}
