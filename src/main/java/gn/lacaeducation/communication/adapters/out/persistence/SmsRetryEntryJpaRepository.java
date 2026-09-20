package gn.lacaeducation.communication.adapters.out.persistence;

import gn.lacaeducation.communication.domain.SmsRetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

interface SmsRetryEntryJpaRepository extends JpaRepository<SmsRetryEntryJpaEntity, Long> {
    List<SmsRetryEntryJpaEntity> findByTenantId(Long tenantId);
    List<SmsRetryEntryJpaEntity> findByStatusAndNextAttemptAtBefore(SmsRetryStatus status, LocalDateTime before);
}
