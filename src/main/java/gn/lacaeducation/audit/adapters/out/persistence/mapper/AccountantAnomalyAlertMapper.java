package gn.lacaeducation.audit.adapters.out.persistence.mapper;

import gn.lacaeducation.audit.adapters.out.persistence.AccountantAnomalyAlertJpaEntity;
import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountantAnomalyAlertMapper {
    AccountantAnomalyAlert toDomain(AccountantAnomalyAlertJpaEntity entity);
    AccountantAnomalyAlertJpaEntity toEntity(AccountantAnomalyAlert domain);
}
