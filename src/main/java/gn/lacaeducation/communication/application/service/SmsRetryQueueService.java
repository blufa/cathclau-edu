package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.SmsRetryQueueUseCase;
import gn.lacaeducation.communication.application.port.out.SmsRetryQueueRepository;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SmsRetryQueueService implements SmsRetryQueueUseCase {

    private final SmsRetryQueueRepository repo;

    @Override
    public SmsRetryEntry enqueue(EnqueueCommand cmd) {
        return repo.save(SmsRetryEntry.builder()
                .tenantId(cmd.tenantId()).recipientId(cmd.recipientId())
                .destination(cmd.destination()).subject(cmd.subject()).content(cmd.content())
                .status(SmsRetryStatus.PENDING).attemptCount(0).nextAttemptAt(LocalDateTime.now())
                .lastError(cmd.lastError()).createdAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmsRetryEntry> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public SmsRetryEntry replay(Long id) {
        SmsRetryEntry e = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SmsRetryEntry", id));
        e.setStatus(SmsRetryStatus.PENDING);
        e.setAttemptCount(0);
        e.setNextAttemptAt(LocalDateTime.now());
        return repo.save(e);
    }
}
