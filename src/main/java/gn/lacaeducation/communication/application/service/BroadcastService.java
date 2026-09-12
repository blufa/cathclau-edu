package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.BroadcastUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.RecipientType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BroadcastService implements BroadcastUseCase {

    private final RecipientResolverService recipientResolverService;
    private final CommunicationUseCase communicationUseCase;

    @Override
    public Communication broadcast(BroadcastCommand c) {
        boolean needsClass = c.audience() == BroadcastAudience.CLASS_PARENTS
                || c.audience() == BroadcastAudience.CLASS_PARENTS_BOYS
                || c.audience() == BroadcastAudience.CLASS_PARENTS_GIRLS;
        if (needsClass && c.classId() == null) {
            throw new IllegalArgumentException("classId est requis pour l'audience " + c.audience());
        }
        if (c.audience() == BroadcastAudience.STUDENT_PARENTS && c.studentId() == null) {
            throw new IllegalArgumentException("studentId est requis pour l'audience STUDENT_PARENTS");
        }

        List<RecipientTarget> recipients = recipientResolverService.resolve(
                c.tenantId(), c.audience(), c.classId(), c.studentId(), c.channel());
        if (recipients.isEmpty()) {
            return null;
        }

        RecipientType recipientType = switch (c.audience()) {
            case ALL_TEACHERS -> RecipientType.TEACHER;
            case ALL_STAFF -> RecipientType.GROUP;
            default -> RecipientType.PARENT;
        };

        return communicationUseCase.send(new CommunicationUseCase.SendCommunicationCommand(
                c.tenantId(), c.senderId(), c.channel(), recipientType,
                c.subject(), c.content(), c.reason(), recipients));
    }
}
