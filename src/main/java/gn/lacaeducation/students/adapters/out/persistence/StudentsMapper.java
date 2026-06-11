package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import gn.lacaeducation.students.domain.StudentParent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface StudentsMapper {
    Student toDomain(EleveJpaEntity entity);
    EleveJpaEntity toEntity(Student domain);
    Enrollment toDomain(InscriptionJpaEntity entity);
    InscriptionJpaEntity toEntity(Enrollment domain);
    StudentDocument toDomain(DocumentEleveJpaEntity entity);
    DocumentEleveJpaEntity toEntity(StudentDocument domain);
    Parent toDomain(ParentJpaEntity entity);
    ParentJpaEntity toEntity(Parent domain);
    StudentParent toDomain(EleveParentJpaEntity entity);
    EleveParentJpaEntity toEntity(StudentParent domain);
}
