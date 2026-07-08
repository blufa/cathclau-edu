package gn.lacaeducation.inbox.application.service;

import gn.lacaeducation.inbox.application.port.in.NotificationUseCase.CreateNotificationCommand;
import gn.lacaeducation.inbox.application.port.out.NotificationRepository;
import gn.lacaeducation.inbox.domain.Notification;
import gn.lacaeducation.inbox.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;
    @InjectMocks NotificationService service;

    @Test
    void create_persistsUnread() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification n = service.create(new CreateNotificationCommand(
                1L, 7L, NotificationType.REPORT_CARD, "Bulletin", "dispo", "/x"));

        assertThat(n.isRead()).isFalse();
        assertThat(n.getUserId()).isEqualTo(7L);
        assertThat(n.getCreatedAt()).isNotNull();
    }

    @Test
    void markRead_ownNotification_marks() {
        when(repo.findById(5L)).thenReturn(Optional.of(Notification.builder().id(5L).userId(7L).build()));

        service.markRead(5L, 7L);

        verify(repo).markRead(5L);
    }

    @Test
    void markRead_otherUsersNotification_throws() {
        when(repo.findById(5L)).thenReturn(Optional.of(Notification.builder().id(5L).userId(99L).build()));

        assertThatThrownBy(() -> service.markRead(5L, 7L)).isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).markRead(anyLong());
    }
}
