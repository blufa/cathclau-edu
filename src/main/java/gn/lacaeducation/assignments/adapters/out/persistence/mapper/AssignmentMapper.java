package gn.lacaeducation.assignments.adapters.out.persistence.mapper;

import gn.lacaeducation.assignments.adapters.out.persistence.AssignmentJpaEntity;
import gn.lacaeducation.assignments.adapters.out.persistence.AssignmentSubmissionJpaEntity;
import gn.lacaeducation.assignments.domain.Assignment;
import gn.lacaeducation.assignments.domain.AssignmentSubmission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {
    Assignment toDomain(AssignmentJpaEntity e);
    AssignmentJpaEntity toEntity(Assignment d);

    AssignmentSubmission toDomain(AssignmentSubmissionJpaEntity e);
    AssignmentSubmissionJpaEntity toEntity(AssignmentSubmission d);
}
