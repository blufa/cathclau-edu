package gn.lacaeducation.staff.application.port.out;

import java.util.List;

public interface TeacherLevelRepository {
    List<Long> findLevelIds(Long teacherId);
    /** Ids des enseignants habilités à enseigner à un niveau donné. */
    List<Long> findTeacherIds(Long levelId);
    /** Remplace l'ensemble des niveaux affectés à l'enseignant. */
    void replace(Long teacherId, Long tenantId, List<Long> levelIds);
}
