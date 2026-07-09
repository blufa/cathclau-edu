package gn.lacaeducation.inbox.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.inbox.application.port.in.NotificationUseCase;
import gn.lacaeducation.inbox.application.port.out.NotificationRepository;
import gn.lacaeducation.inbox.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service("inboxNotificationService")
@Transactional
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepository repo;

    @Override
    public Notification create(CreateNotificationCommand c) {
        return repo.save(Notification.builder()
                .tenantId(c.tenantId()).userId(c.userId()).type(c.type())
                .title(c.title()).content(c.content()).link(c.link())
                .read(false).createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findForUser(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repo.countUnread(userId);
    }

    @Override
    public void markRead(Long id, Long userId) {
        Notification n = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        // Un utilisateur ne peut marquer comme lues que ses propres notifications.
        if (userId != null && !userId.equals(n.getUserId())) {
            throw new AccessDeniedException("Cette notification ne vous appartient pas.");
        }
        repo.markRead(id);
    }

    @Override
    public void markAllRead(Long userId) {
        repo.markAllRead(userId);
    }
}
