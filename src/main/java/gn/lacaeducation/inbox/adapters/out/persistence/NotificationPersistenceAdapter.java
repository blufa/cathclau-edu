package gn.lacaeducation.inbox.adapters.out.persistence;

import gn.lacaeducation.inbox.application.port.out.NotificationRepository;
import gn.lacaeducation.inbox.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    @Override
    public Notification save(Notification n) {
        return toDomain(jpa.save(toEntity(n)));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public long countUnread(Long userId) {
        return jpa.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markRead(Long id) {
        jpa.markRead(id);
    }

    @Override
    public void markAllRead(Long userId) {
        jpa.markAllRead(userId);
    }

    private Notification toDomain(NotificationJpaEntity e) {
        return Notification.builder()
                .id(e.getId()).tenantId(e.getTenantId()).userId(e.getUserId()).type(e.getType())
                .title(e.getTitle()).content(e.getContent()).link(e.getLink())
                .read(e.isRead()).createdAt(e.getCreatedAt())
                .build();
    }

    private NotificationJpaEntity toEntity(Notification d) {
        NotificationJpaEntity e = new NotificationJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setUserId(d.getUserId()); e.setType(d.getType());
        e.setTitle(d.getTitle()); e.setContent(d.getContent()); e.setLink(d.getLink());
        e.setRead(d.isRead()); e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
