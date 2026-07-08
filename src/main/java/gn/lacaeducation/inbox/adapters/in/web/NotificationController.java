package gn.lacaeducation.inbox.adapters.in.web;

import gn.lacaeducation.inbox.application.port.in.NotificationUseCase;
import gn.lacaeducation.inbox.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cloche de notifications in-app (§13) : chaque utilisateur consulte et marque comme lues ses propres
 * notifications.
 */
@Tag(name = "Notifications", description = "Notifications in-app de l'utilisateur connecté (§13)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase useCase;

    @Operation(summary = "Mes notifications (les plus récentes en premier)")
    @GetMapping
    public List<Notification> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        return useCase.findForUser(currentUserId(jwt));
    }

    @Operation(summary = "Nombre de notifications non lues (badge de la cloche)")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", useCase.unreadCount(currentUserId(jwt)));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PatchMapping("/{id}/lu")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        useCase.markRead(id, currentUserId(jwt));
    }

    @Operation(summary = "Marquer toutes mes notifications comme lues")
    @PatchMapping("/lues")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        useCase.markAllRead(currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
