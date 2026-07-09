package gn.lacaeducation.staff.application.port.out;

import java.util.List;

public interface TeacherSubjectRepository {
    List<Long> findSubjectIds(Long teacherId);
    /** Ids des enseignants affectés à une matière donnée. */
    List<Long> findTeacherIds(Long subjectId);
    /** Remplace l'ensemble des matières affectées à l'enseignant. */
    void replace(Long teacherId, Long tenantId, List<Long> subjectIds);
}
