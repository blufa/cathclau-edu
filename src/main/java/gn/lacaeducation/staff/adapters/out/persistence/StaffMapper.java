package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.domain.Teacher;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface StaffMapper {
    Teacher toDomain(EnseignantJpaEntity entity);
    EnseignantJpaEntity toEntity(Teacher domain);
}
