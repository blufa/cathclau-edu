package gn.lacaeducation.staff.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.staff.application.port.in.EmployeeUseCase;
import gn.lacaeducation.staff.application.port.in.TeacherUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Recherche croisée enseignant/personnel par nom (§retour comptable, refonte inscription, point 25) —
 * pour proposer "Lier comme parent" lors de l'inscription, avec pré-remplissage nom/téléphone. Le
 * {@code userId} renvoyé est celui à réutiliser tel quel pour le nouveau compte {@code Parent}, faute
 * de quoi la détection automatique de l'avantage personnel (voir {@code StaffChildBenefitService})
 * ne fonctionnerait pas pour ce parent.
 */
@Tag(name = "Staff Search", description = "Recherche croisée enseignant/personnel par nom")
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffSearchController {

    private final TeacherUseCase teacherUseCase;
    private final EmployeeUseCase employeeUseCase;

    public record StaffSearchResult(Long userId, String staffType, Long staffId, String fullName, String phone) {}

    @Operation(summary = "Rechercher un enseignant/employé existant par nom, pour le lier comme parent")
    @PreAuthorize("hasAuthority('parent:manage')")
    @GetMapping("/search")
    public List<StaffSearchResult> search(@RequestParam String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        if (needle.isBlank()) return List.of();
        Long tenantId = TenantContext.get();
        List<StaffSearchResult> results = new ArrayList<>();
        teacherUseCase.findByTenantId(tenantId).stream()
                .filter(t -> t.getUserId() != null && matches(t.getFirstName(), t.getLastName(), needle))
                .forEach(t -> results.add(new StaffSearchResult(t.getUserId(), "TEACHER", t.getId(),
                        fullName(t.getFirstName(), t.getLastName()), t.getPhone())));
        employeeUseCase.findByTenant(tenantId).stream()
                .filter(e -> e.getUserId() != null && matches(e.getFirstName(), e.getLastName(), needle))
                .forEach(e -> results.add(new StaffSearchResult(e.getUserId(), "EMPLOYEE", e.getId(),
                        fullName(e.getFirstName(), e.getLastName()), e.getPhone())));
        return results;
    }

    private static boolean matches(String first, String last, String needle) {
        String full = ((first == null ? "" : first) + " " + (last == null ? "" : last)).toLowerCase();
        return full.contains(needle);
    }

    private static String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
