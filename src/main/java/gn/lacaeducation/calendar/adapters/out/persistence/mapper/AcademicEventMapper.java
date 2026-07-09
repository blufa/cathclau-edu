package gn.lacaeducation.calendar.adapters.out.persistence.mapper;

import gn.lacaeducation.calendar.adapters.out.persistence.AcademicEventJpaEntity;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcademicEventMapper {
    AcademicEvent toDomain(AcademicEventJpaEntity e);
    AcademicEventJpaEntity toEntity(AcademicEvent d);
}
