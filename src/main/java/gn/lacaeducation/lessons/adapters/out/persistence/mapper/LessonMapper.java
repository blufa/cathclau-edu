package gn.lacaeducation.lessons.adapters.out.persistence.mapper;

import gn.lacaeducation.lessons.adapters.out.persistence.LessonEntryJpaEntity;
import gn.lacaeducation.lessons.domain.LessonEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LessonMapper {
    LessonEntry toDomain(LessonEntryJpaEntity e);
    LessonEntryJpaEntity toEntity(LessonEntry d);
}
