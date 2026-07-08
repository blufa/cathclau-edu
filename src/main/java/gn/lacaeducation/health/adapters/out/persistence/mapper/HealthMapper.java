package gn.lacaeducation.health.adapters.out.persistence.mapper;

import gn.lacaeducation.health.adapters.out.persistence.HealthRecordJpaEntity;
import gn.lacaeducation.health.adapters.out.persistence.InfirmaryVisitJpaEntity;
import gn.lacaeducation.health.domain.HealthRecord;
import gn.lacaeducation.health.domain.InfirmaryVisit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HealthMapper {
    HealthRecord toDomain(HealthRecordJpaEntity e);
    HealthRecordJpaEntity toEntity(HealthRecord d);

    InfirmaryVisit toDomain(InfirmaryVisitJpaEntity e);
    InfirmaryVisitJpaEntity toEntity(InfirmaryVisit d);
}
