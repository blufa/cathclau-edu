package gn.lacaeducation.dpe.adapters.out.persistence.mapper;

import gn.lacaeducation.dpe.adapters.out.persistence.DpeAlertJpaEntity;
import gn.lacaeducation.dpe.domain.DpeAlert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DpeMapper {
    DpeAlert toDomain(DpeAlertJpaEntity entity);
    DpeAlertJpaEntity toEntity(DpeAlert domain);
}
