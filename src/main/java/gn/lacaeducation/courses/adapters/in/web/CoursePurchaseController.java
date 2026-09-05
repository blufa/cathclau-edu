package gn.lacaeducation.courses.adapters.in.web;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.courses.application.port.in.CoursePurchaseUseCase;
import gn.lacaeducation.courses.domain.CoursePurchase;
import gn.lacaeducation.courses.domain.CourseResource;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Achat de cours par le parent ou l'élève connecté (§16 B). L'achat émet une facture normale
 * (module billing) ; le paiement effectif se fait ensuite via le circuit existant
 * {@code POST /api/v1/paiements} (Djomy) avec l'identifiant de facture renvoyé par
 * {@link #purchase}. Le contenu (URLs vidéo/PDF) n'est révélé qu'une fois la facture soldée.
 */
@Tag(name = "Course Purchases", description = "Achat et accès aux cours (portail parent/élève)")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CoursePurchaseController {

    private final CoursePurchaseUseCase useCase;
    private final ParentAccessProvider parentAccessProvider;
    private final StudentDataProvider studentDataProvider;

    public record PurchaseRequest(Long studentId) {}

    @Operation(summary = "Initier l'achat d'un cours pour un élève (émet la facture à régler)")
    @PreAuthorize("hasAnyRole('PARENT','STUDENT')")
    @PostMapping("/{id}/achat")
    public CoursePurchase purchase(@PathVariable Long id, @RequestBody(required = false) PurchaseRequest r,
                                   @AuthenticationPrincipal Jwt jwt) {
        Long studentId = resolveStudentId(jwt, r == null ? null : r.studentId());
        return useCase.purchase(new CoursePurchaseUseCase.PurchaseCommand(TenantContext.get(), id, studentId));
    }

    @Operation(summary = "Mes achats de cours (élève connecté, ou enfant précisé par un parent)")
    @PreAuthorize("hasAnyRole('PARENT','STUDENT')")
    @GetMapping("/mes-achats")
    public List<CoursePurchase> myPurchases(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam(required = false) Long studentId) {
        return useCase.findByStudentId(resolveStudentId(jwt, studentId));
    }

    @Operation(summary = "Contenu d'un cours : URLs déverrouillées si acheté, aperçu sinon")
    @PreAuthorize("hasAnyRole('PARENT','STUDENT')")
    @GetMapping("/{id}/contenu")
    public List<CourseResource> content(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
                                        @RequestParam(required = false) Long studentId) {
        return useCase.accessibleResources(resolveStudentId(jwt, studentId), id);
    }

    @Operation(summary = "Suivi de tous les achats de cours du tenant (comptabilité/direction)")
    @PreAuthorize("hasAuthority('cours:manage')")
    @GetMapping("/achats")
    public List<CoursePurchase> allPurchases() {
        return useCase.findByTenantId(TenantContext.get());
    }

    /** Élève connecté : résolu depuis son propre compte. Parent connecté : {@code studentId} requis et vérifié. */
    private Long resolveStudentId(Jwt jwt, Long requestedStudentId) {
        Long userId = currentUserId(jwt);
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && roles.contains("STUDENT")) {
            return studentDataProvider.findStudentIdByUserId(userId)
                    .orElseThrow(() -> new BusinessRuleException("Compte élève introuvable."));
        }
        if (requestedStudentId == null) {
            throw new BusinessRuleException("Précisez l'élève concerné (studentId).");
        }
        if (!parentAccessProvider.isParentOfStudent(userId, requestedStudentId)) {
            throw new BusinessRuleException("Cet élève n'est pas rattaché à votre compte.");
        }
        return requestedStudentId;
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
