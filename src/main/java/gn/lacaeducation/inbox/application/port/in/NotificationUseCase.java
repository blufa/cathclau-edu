package gn.lacaeducation.inbox.application.port.in;

import gn.lacaeducation.inbox.domain.Notification;
import gn.lacaeducation.inbox.domain.NotificationType;

import java.util.List;

public interface NotificationUseCase {
    record CreateNotificationCommand(Long tenantId, Long userId, NotificationType type,
                                     String title, String content, String link) {}

    /** Crée une notification pour un utilisateur (usage interne, sur événement métier). */
    Notification create(CreateNotificationCommand command);

    List<Notification> findForUser(Long userId);
    long unreadCount(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
}
