package gn.lacaeducation.inbox.application.port.out;

import gn.lacaeducation.inbox.domain.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findByUserId(Long userId);
    long countUnread(Long userId);
    void markRead(Long id);
    void markAllRead(Long userId);
}
