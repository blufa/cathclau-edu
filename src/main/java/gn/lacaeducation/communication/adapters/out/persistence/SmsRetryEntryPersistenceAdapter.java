package gn.lacaeducation.communication.adapters.out.persistence;

import gn.lacaeducation.communication.application.port.out.SmsRetryQueueRepository;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SmsRetryEntryPersistenceAdapter implements SmsRetryQueueRepository {

    private final SmsRetryEntryJpaRepository jpa;

    @Override
    public SmsRetryEntry save(SmsRetryEntry e) {
        return toDomain(jpa.save(toEntity(e)));
    }

    @Override
    public Optional<SmsRetryEntry> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<SmsRetryEntry> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<SmsRetryEntry> findByStatusAndNextAttemptAtBefore(SmsRetryStatus status, LocalDateTime before) {
        return jpa.findByStatusAndNextAttemptAtBefore(status, before).stream().map(this::toDomain).toList();
    }

    private SmsRetryEntryJpaEntity toEntity(SmsRetryEntry d) {
        SmsRetryEntryJpaEntity e = new SmsRetryEntryJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setRecipientId(d.getRecipientId());
        e.setDestination(d.getDestination());
        e.setSubject(d.getSubject());
        e.setContent(d.getContent());
        e.setStatus(d.getStatus());
        e.setAttemptCount(d.getAttemptCount());
        e.setNextAttemptAt(d.getNextAttemptAt());
        e.setLastError(d.getLastError());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private SmsRetryEntry toDomain(SmsRetryEntryJpaEntity e) {
        return SmsRetryEntry.builder()
                .id(e.getId()).tenantId(e.getTenantId()).recipientId(e.getRecipientId())
                .destination(e.getDestination()).subject(e.getSubject()).content(e.getContent())
                .status(e.getStatus()).attemptCount(e.getAttemptCount()).nextAttemptAt(e.getNextAttemptAt())
                .lastError(e.getLastError()).createdAt(e.getCreatedAt())
                .build();
    }
}
