package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.domain.*;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface CommunicationMapper {
    Communication toDomain(CommunicationJpaEntity e);
    CommunicationJpaEntity toEntity(Communication d);
    MessageRecipient toDomain(MessageRecipientJpaEntity e);
    MessageRecipientJpaEntity toEntity(MessageRecipient d);
    MessageTemplate toDomain(MessageTemplateJpaEntity e);
    MessageTemplateJpaEntity toEntity(MessageTemplate d);
    ScheduledBroadcast toDomain(ScheduledBroadcastJpaEntity e);
    ScheduledBroadcastJpaEntity toEntity(ScheduledBroadcast d);
}
