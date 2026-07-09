package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.domain.*;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface GradingMapper {
    Assessment toDomain(AssessmentJpaEntity e);
    AssessmentJpaEntity toEntity(Assessment d);
    Grade toDomain(GradeJpaEntity e);
    GradeJpaEntity toEntity(Grade d);
    Exam toDomain(ExamJpaEntity e);
    ExamJpaEntity toEntity(Exam d);
    ReportCard toDomain(ReportCardJpaEntity e);
    ReportCardJpaEntity toEntity(ReportCard d);
    ReportCardLine toDomain(ReportCardLineJpaEntity e);
    ReportCardLineJpaEntity toEntity(ReportCardLine d);
    GradeHistory toDomain(GradeHistoryJpaEntity e);
    GradeHistoryJpaEntity toEntity(GradeHistory d);
}
