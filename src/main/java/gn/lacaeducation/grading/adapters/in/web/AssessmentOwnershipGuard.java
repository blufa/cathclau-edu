package gn.lacaeducation.grading.adapters.in.web;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/** Un enseignant ne peut saisir, modifier ou supprimer les notes/évaluations que de celles qu'il a
 * lui-même créées — il consulte librement celles des autres professeurs dans les mêmes classes,
 * jamais y écrire (§retour fondateur : un principal ne retrouvait pas les notes d'un professeur,
 * réparti par erreur entre plusieurs évaluations ; par la même occasion, rien n'empêchait un
 * professeur de modifier les notes saisies par un collègue). SCHOOL_ADMIN/DIRECTOR ne sont jamais
 * concernés par cette restriction, même avec un profil enseignant lié par ailleurs. */
@Component
@RequiredArgsConstructor
class AssessmentOwnershipGuard {

    private final AssessmentUseCase assessmentUseCase;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;

    void requireOwnerOrAdmin(Long assessmentId, Long currentUserId, Authentication authentication) {
        if (isAdminOrDirector(authentication)) return;
        Assessment a = assessmentUseCase.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", assessmentId));
        Long actingTeacherId = teacherPublicInfoProvider.findTeacherIdByUserId(currentUserId).orElse(null);
        if (actingTeacherId == null || !actingTeacherId.equals(a.getTeacherId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez saisir ou modifier que les évaluations que vous avez créées.");
        }
    }

    static boolean isAdminOrDirector(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SCHOOL_ADMIN") || a.equals("ROLE_DIRECTOR"));
    }
}
