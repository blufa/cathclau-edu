package gn.lacaeducation.staff.application.port.out;

import java.util.List;

public interface TeacherClassRepository {
    List<Long> findClassIds(Long teacherId);
    /** Remplace l'ensemble des classes affectées à l'enseignant. */
    void replace(Long teacherId, Long tenantId, List<Long> classIds);
}
