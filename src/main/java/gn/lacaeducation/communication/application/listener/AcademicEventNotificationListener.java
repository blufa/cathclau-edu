package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.calendar.application.event.AcademicEventCreatedEvent;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.application.port.in.BroadcastUseCase.BroadcastCommand;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/**
 * À la création d'un événement du calendrier académique (réunion, sortie, composition, échéance…),
 * diffuse une notification SMS à tous les parents de l'établissement (§18 : chaque événement métier
 * notifie les parents concernés) — réutilise la diffusion de masse segmentée déjà construite
 * ({@link BroadcastUseCase}, audience {@link BroadcastAudience#ALL_PARENTS}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AcademicEventNotificationListener {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BroadcastUseCase broadcastUseCase;

    @TransactionalEventListener
    @Async
    public void onAcademicEventCreated(AcademicEventCreatedEvent event) {
        String content = "Nouvel événement : " + event.title() + " le " + event.startDate().format(DATE_FMT)
                + (event.description() != null && !event.description().isBlank() ? " — " + event.description() : "");
        broadcastUseCase.broadcast(new BroadcastCommand(event.tenantId(), null, CommunicationChannel.SMS,
                BroadcastAudience.ALL_PARENTS, null, null, event.title(), content, mapReason(event.eventType())));
        log.info("Diffusion événement calendrier id={} '{}' vers tous les parents (tenant={})",
                event.eventId(), event.title(), event.tenantId());
    }

    private CommunicationReason mapReason(String eventType) {
        return switch (eventType) {
            case "MEETING" -> CommunicationReason.MEETING;
            case "EXAM" -> CommunicationReason.EXAM;
            case "FEE_DUE" -> CommunicationReason.PAYMENT_DUE;
            case "RESULT" -> CommunicationReason.RESULT;
            default -> CommunicationReason.GENERAL;
        };
    }
}
