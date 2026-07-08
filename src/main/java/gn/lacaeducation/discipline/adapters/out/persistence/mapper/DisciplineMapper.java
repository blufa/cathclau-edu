package gn.lacaeducation.discipline.adapters.out.persistence.mapper;

import gn.lacaeducation.discipline.adapters.out.persistence.DisciplineIncidentJpaEntity;
import gn.lacaeducation.discipline.domain.DisciplineIncident;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DisciplineMapper {
    DisciplineIncident toDomain(DisciplineIncidentJpaEntity e);
    DisciplineIncidentJpaEntity toEntity(DisciplineIncident d);
}
