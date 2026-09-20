package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.ScheduledBroadcastStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
interface ScheduledBroadcastJpaRepository extends JpaRepository<ScheduledBroadcastJpaEntity, Long> {
    List<ScheduledBroadcastJpaEntity> findByTenantId(Long tenantId);
    List<ScheduledBroadcastJpaEntity> findByTenantIdAndStatusAndSendAtLessThanEqual(
            Long tenantId, ScheduledBroadcastStatus status, LocalDateTime sendAt);
}
