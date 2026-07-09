package gn.lacaeducation.transport.adapters.out.persistence.mapper;

import gn.lacaeducation.transport.adapters.out.persistence.TransportRouteJpaEntity;
import gn.lacaeducation.transport.adapters.out.persistence.TransportStopJpaEntity;
import gn.lacaeducation.transport.adapters.out.persistence.TransportSubscriptionJpaEntity;
import gn.lacaeducation.transport.domain.TransportRoute;
import gn.lacaeducation.transport.domain.TransportStop;
import gn.lacaeducation.transport.domain.TransportSubscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransportMapper {
    TransportRoute toDomain(TransportRouteJpaEntity e);
    TransportRouteJpaEntity toEntity(TransportRoute d);

    TransportStop toDomain(TransportStopJpaEntity e);
    TransportStopJpaEntity toEntity(TransportStop d);

    TransportSubscription toDomain(TransportSubscriptionJpaEntity e);
    TransportSubscriptionJpaEntity toEntity(TransportSubscription d);
}
