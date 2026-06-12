package gn.lacaeducation.academics;
import java.util.List;
import java.util.Optional;
public interface AcademicsDataProvider {
    Optional<ClassPublicInfo> findClass(Long classId);
    Optional<PeriodPublicInfo> findPeriod(Long periodId);
    List<ClassSubjectPublicInfo> findClassSubjects(Long classId);
}
