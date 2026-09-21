package gn.lacaeducation.studentcards.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.studentcards.application.service.CardTemplateService;
import gn.lacaeducation.studentcards.domain.BadgeKind;
import gn.lacaeducation.studentcards.domain.CardOrientation;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Card Templates", description = "Modèles de cartes scolaires")
@RestController
@RequestMapping("/api/v1/cartes/modeles")
@RequiredArgsConstructor
public class CardTemplateController {

    private final CardTemplateService service;

    public record TemplateRequest(String name, String logoUrl, String primaryColor, String secondaryColor,
                                  String backgroundUrl, CardOrientation orientation, String rectoConfig,
                                  String versoConfig, Boolean active, BadgeKind badgeKind) {}

    @Operation(summary = "Lister les modèles de cartes (filtrage possible par type de badge)")
    @GetMapping
    public List<CardTemplate> list(@RequestParam(required = false) BadgeKind badgeKind) {
        List<CardTemplate> all = service.findByTenant(TenantContext.get());
        return badgeKind == null ? all : all.stream().filter(t -> t.getBadgeKind() == badgeKind).toList();
    }

    @Operation(summary = "Créer un modèle de carte")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PostMapping
    public CardTemplate create(@RequestBody TemplateRequest r) {
        return service.save(CardTemplate.builder().tenantId(TenantContext.get())
                .name(r.name()).logoUrl(r.logoUrl()).primaryColor(r.primaryColor()).secondaryColor(r.secondaryColor())
                .backgroundUrl(r.backgroundUrl()).orientation(r.orientation()).rectoConfig(r.rectoConfig())
                .versoConfig(r.versoConfig()).active(r.active())
                .badgeKind(r.badgeKind() == null ? BadgeKind.STUDENT : r.badgeKind()).build());
    }

    @Operation(summary = "Modifier un modèle de carte")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PutMapping("/{id}")
    public CardTemplate update(@PathVariable Long id, @RequestBody TemplateRequest r) {
        return service.update(id, CardTemplate.builder()
                .name(r.name()).logoUrl(r.logoUrl()).primaryColor(r.primaryColor()).secondaryColor(r.secondaryColor())
                .backgroundUrl(r.backgroundUrl()).orientation(r.orientation()).rectoConfig(r.rectoConfig())
                .versoConfig(r.versoConfig()).active(r.active())
                .badgeKind(r.badgeKind() == null ? BadgeKind.STUDENT : r.badgeKind()).build());
    }

    @Operation(summary = "Définir un modèle comme modèle par défaut")
    @PreAuthorize("hasAuthority('carte:manage')")
    @PatchMapping("/{id}/definir-defaut")
    public CardTemplate setDefault(@PathVariable Long id) { return service.setDefault(TenantContext.get(), id); }

    @Operation(summary = "Supprimer un modèle de carte")
    @PreAuthorize("hasAuthority('carte:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
