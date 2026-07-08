package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ScheduleSlotPublicInfo;
import gn.lacaeducation.common.PortalAccess;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Emploi du temps d'un élève (portail élève / parent) : résout la classe courante de l'élève puis
 * récupère ses créneaux via l'API publique du module academics. Placé dans le module portal pour éviter tout cycle (students ↔ academics).
 */
@Tag(name = "Student Schedule", description = "Emploi du temps de la classe courante d'un élève")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentScheduleController {

    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final ParentAccessProvider parentAccessProvider;

    @Operation(summary = "Emploi du temps de la classe courante d'un élève")
    @GetMapping("/{studentId}/emploi-du-temps")
    public List<ScheduleSlotPublicInfo> schedule(@PathVariable Long studentId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        assertParentScope(jwt, studentId);
        return studentDataProvider.findCurrentClassId(studentId)
                .map(academicsDataProvider::findScheduleByClass)
                .orElseGet(List::of);
    }

    private void assertParentScope(Jwt jwt, Long studentId) {
        if (jwt == null) return;
        Long userId;
        try { userId = Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { userId = null; }
        PortalAccess.assertParentCanAccessStudent(jwt.getClaimAsStringList("roles"), userId, studentId,
                parentAccessProvider::isParentOfStudent);
    }
}
