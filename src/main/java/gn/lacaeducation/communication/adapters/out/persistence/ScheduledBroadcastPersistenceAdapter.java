package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.application.port.out.ScheduledBroadcastRepository;
import gn.lacaeducation.communication.domain.ScheduledBroadcast;
import gn.lacaeducation.communication.domain.ScheduledBroadcastStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ScheduledBroadcastPersistenceAdapter implements ScheduledBroadcastRepository {
    private final ScheduledBroadcastJpaRepository jpa;
    private final CommunicationMapper mapper;
    @Override public ScheduledBroadcast save(ScheduledBroadcast b) { return mapper.toDomain(jpa.save(mapper.toEntity(b))); }
    @Override public Optional<ScheduledBroadcast> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ScheduledBroadcast> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<ScheduledBroadcast> findDuePending(Long t, LocalDateTime now) {
        return jpa.findByTenantIdAndStatusAndSendAtLessThanEqual(t, ScheduledBroadcastStatus.PENDING, now)
                .stream().map(mapper::toDomain).toList();
    }
}
