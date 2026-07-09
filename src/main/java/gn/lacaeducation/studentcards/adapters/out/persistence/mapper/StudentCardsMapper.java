package gn.lacaeducation.studentcards.adapters.out.persistence.mapper;

import gn.lacaeducation.studentcards.adapters.out.persistence.CardTemplateJpaEntity;
import gn.lacaeducation.studentcards.adapters.out.persistence.StudentCardJpaEntity;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import gn.lacaeducation.studentcards.domain.StudentCard;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentCardsMapper {
    CardTemplate toDomain(CardTemplateJpaEntity e);
    CardTemplateJpaEntity toEntity(CardTemplate d);
    StudentCard toDomain(StudentCardJpaEntity e);
    StudentCardJpaEntity toEntity(StudentCard d);
}
