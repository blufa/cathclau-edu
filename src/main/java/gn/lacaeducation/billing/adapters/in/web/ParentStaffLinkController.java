package gn.lacaeducation.billing.adapters.in.web;

import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ce parent est-il lié à un membre du personnel (§retour comptable, refonte inscription, point 25) —
 * pour proposer automatiquement la remise personnel à l'inscription. Vit dans {@code billing} (et non
 * {@code students}, propriétaire de la ressource REST {@code /parents}) car la détection dépend à la
 * fois de {@code students} et de {@code staff} — les placer dans {@code students} créerait un cycle
 * de dépendances entre modules ({@code students} → {@code staff} → {@code grading} → {@code students}
 * via {@code EnseignantService}/{@code GradingTeacherProvider}). Réutilise exactement la même
 * détection par {@code userId} partagé que {@link gn.lacaeducation.billing.application.service.StaffChildBenefitService}.
 */
@Tag(name = "Parent Staff Link", description = "Détection du lien parent ↔ personnel (par userId partagé)")
@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentStaffLinkController {

    private final ParentAccessProvider parentAccessProvider;
    private final TeacherPublicInfoProvider teacherProvider;
    private final EmployeePublicInfoProvider employeeProvider;

    public record StaffLinkInfo(String staffType, Long staffId) {}

    @Operation(summary = "Indique si ce parent est lié à un membre du personnel (enseignant ou employé) — par compte partagé OU par lien direct sans compte")
    @PreAuthorize("hasAuthority('parent:manage')")
    @GetMapping("/{id}/staff-link")
    public ResponseEntity<StaffLinkInfo> staffLink(@PathVariable Long id) {
        Long userId = parentAccessProvider.findUserIdByParentId(id).orElse(null);
        if (userId != null) {
            var viaAccount = teacherProvider.findTeacherIdByUserId(userId)
                    .map(tid -> new StaffLinkInfo("TEACHER", tid))
                    .or(() -> employeeProvider.findEmployeeIdByUserId(userId).map(eid -> new StaffLinkInfo("EMPLOYEE", eid)));
            if (viaAccount.isPresent()) return ResponseEntity.ok(viaAccount.get());
        }
        // Personnel sans compte (ex. pas de smartphone Android) : lien direct sur la fiche Parent.
        return parentAccessProvider.findDirectStaffLinkByParentId(id)
                .map(link -> new StaffLinkInfo(link.staffType(), link.staffId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }
}
