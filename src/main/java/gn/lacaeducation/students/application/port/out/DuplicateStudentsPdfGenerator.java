package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.application.service.DuplicateDetectionService.Group;

import java.util.List;

/** Impression de la liste des groupes de doublons détectés (§retour comptable) — même en-tête que les
 * autres documents administratifs, voir {@code AdminDocumentLayout}. */
public interface DuplicateStudentsPdfGenerator {
    byte[] generate(Long tenantId, List<Group> groups);
}
