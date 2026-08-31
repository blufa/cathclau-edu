package gn.lacaeducation.platform.adapters.in.web;

import gn.lacaeducation.platform.adapters.in.web.dto.TenantResponse;
import gn.lacaeducation.platform.application.port.in.PrefectureUseCase;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * SUPER_ADMIN : préfectures/communes (périmètre DPE/DCE), rattachement des écoles et affectation
 * des comptes DPE_ADMIN — fondation du module de partenariat DPE (DPE_ARCHITECTURE_ANALYSIS.md §5-8).
 * Le rôle DPE_ADMIN lui-même se gère via les endpoints rôles/utilisateurs existants ; ce contrôleur
 * ne gère que le périmètre géographique.
 */
@Tag(name = "DPE prefectures", description = "Périmètres DPE/DCE, écoles rattachées, comptes DPE_ADMIN affectés")
@RestController
@RequestMapping("/api/v1/platform/prefectures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PrefectureController {

    private final PrefectureUseCase useCase;

    public record PrefectureRequest(String nom, PrefectureType type, String region, String pays,
                                     BigDecimal latitude, BigDecimal longitude) {}

    public record AssignUserRequest(Long userId) {}

    @GetMapping
    public List<Prefecture> list() {
        return useCase.findAll();
    }

    @GetMapping("/{id}")
    public Prefecture get(@PathVariable Long id) {
        return useCase.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Prefecture create(@RequestBody PrefectureRequest req) {
        return useCase.create(new PrefectureUseCase.PrefectureCommand(
                req.nom(), req.type(), req.region(), req.pays(), req.latitude(), req.longitude()));
    }

    @PutMapping("/{id}")
    public Prefecture update(@PathVariable Long id, @RequestBody PrefectureRequest req) {
        return useCase.update(id, new PrefectureUseCase.PrefectureCommand(
                req.nom(), req.type(), req.region(), req.pays(), req.latitude(), req.longitude()));
    }

    @PatchMapping("/{id}/archiver")
    public Prefecture archive(@PathVariable Long id) {
        return useCase.archive(id);
    }

    @PatchMapping("/{id}/reactiver")
    public Prefecture reactivate(@PathVariable Long id) {
        return useCase.reactivate(id);
    }

    @Operation(summary = "Écoles actuellement rattachées à cette préfecture/commune")
    @GetMapping("/{id}/ecoles")
    public List<TenantResponse> schools(@PathVariable Long id) {
        return useCase.schoolsOf(id).stream().map(TenantResponse::from).toList();
    }

    @Operation(summary = "Rattache une école au périmètre (remplace le rattachement précédent le cas échéant)")
    @PutMapping("/{id}/ecoles/{tenantId}")
    public TenantResponse assignSchool(@PathVariable Long id, @PathVariable Long tenantId) {
        return TenantResponse.from(useCase.assignSchool(id, tenantId));
    }

    @Operation(summary = "Détache une école de son périmètre DPE/DCE")
    @DeleteMapping("/ecoles/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignSchool(@PathVariable Long tenantId) {
        useCase.unassignSchool(tenantId);
    }

    @Operation(summary = "Affecte un compte DPE_ADMIN à cette préfecture/commune")
    @PostMapping("/{id}/utilisateurs")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignUser(@PathVariable Long id, @RequestBody AssignUserRequest req) {
        useCase.assignUser(id, req.userId());
    }

    @Operation(summary = "Retire un compte DPE_ADMIN de cette préfecture/commune")
    @DeleteMapping("/{id}/utilisateurs/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignUser(@PathVariable Long id, @PathVariable Long userId) {
        useCase.unassignUser(id, userId);
    }
}
