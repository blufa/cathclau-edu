package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.domain.BroadcastAudience;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Résout une {@link BroadcastAudience} en une liste de destinataires réels (téléphone ou e-mail selon
 * le canal). Combine les API publiques déjà exposées par {@code students} et {@code staff} — aucun
 * accès direct à leurs repositories.
 */
@Service
@RequiredArgsConstructor
class RecipientResolverService {

    private final StudentDataProvider studentDataProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;

    List<RecipientTarget> resolve(Long tenantId, BroadcastAudience audience, Long classId, Long studentId,
                                  CommunicationChannel channel) {
        if (audience == BroadcastAudience.ALL_TEACHERS) {
            return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                    .map(t -> destinationOf(t.email(), t.phone(), channel))
                    .filter(d -> d != null && !d.isBlank())
                    .map(d -> new RecipientTarget(null, d))
                    .toList();
        }

        // Frais annexe optionnel (§billing) : la diffusion de masse (annonces, etc.) ne touche que les
        // familles ayant accepté l'accès à la plateforme — jamais les autres, quel que soit le public visé.
        List<ParentPublicInfo> parents = switch (audience) {
            case ALL_PARENTS -> studentDataProvider.findAllParentsWithPlatformAccess(tenantId);
            case CLASS_PARENTS -> parentsOfClass(classId, null);
            case CLASS_PARENTS_BOYS -> parentsOfClass(classId, "MALE");
            case CLASS_PARENTS_GIRLS -> parentsOfClass(classId, "FEMALE");
            case STUDENT_PARENTS -> studentDataProvider.hasPlatformAccessOptIn(studentId)
                    ? studentDataProvider.findParentsByStudent(studentId) : List.of();
            case ALL_TEACHERS -> throw new IllegalStateException("traité plus haut");
        };

        return parents.stream()
                .map(p -> destinationOf(p.email(), p.phone(), channel))
                .filter(d -> d != null && !d.isBlank())
                .map(d -> new RecipientTarget(null, d))
                .toList();
    }

    /** Parents des élèves d'une classe, filtrés par sexe si fourni, dédupliqués par identifiant parent
     * (un parent avec plusieurs enfants dans la classe ne reçoit qu'un seul message). */
    private List<ParentPublicInfo> parentsOfClass(Long classId, String genderFilter) {
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(classId);
        Map<Long, ParentPublicInfo> byParentId = new LinkedHashMap<>();
        for (StudentPublicInfo s : students) {
            if (genderFilter != null && !genderFilter.equalsIgnoreCase(s.gender())) continue;
            if (!studentDataProvider.hasPlatformAccessOptIn(s.id())) continue;
            for (ParentPublicInfo p : studentDataProvider.findParentsByStudent(s.id())) {
                byParentId.putIfAbsent(p.id(), p);
            }
        }
        return List.copyOf(byParentId.values());
    }

    private static String destinationOf(String email, String phone, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? email : phone;
    }
}
