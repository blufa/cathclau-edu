package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.domain.*;
import org.mapstruct.Mapper;
import java.time.YearMonth;
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
    MonthlyReportCard toDomain(MonthlyReportCardJpaEntity e);
    MonthlyReportCardJpaEntity toEntity(MonthlyReportCard d);
    MonthlyReportCardLine toDomain(MonthlyReportCardLineJpaEntity e);
    MonthlyReportCardLineJpaEntity toEntity(MonthlyReportCardLine d);

    default String map(YearMonth ym) { return ym == null ? null : ym.toString(); }
    default YearMonth map(String s) { return s == null ? null : YearMonth.parse(s); }
}
