package gn.lacaeducation.studentcards.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.studentcards.application.service.CardTemplateService;
import gn.lacaeducation.studentcards.domain.CardOrientation;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Card Templates", description = "Student-card templates")
@RestController
@RequestMapping("/api/v1/cartes/modeles")
@RequiredArgsConstructor
public class CardTemplateController {

    private final CardTemplateService service;

    public record TemplateRequest(String name, String logoUrl, String primaryColor, String secondaryColor,
                                  String backgroundUrl, CardOrientation orientation, String rectoConfig,
                                  String versoConfig, Boolean active) {}

    @Operation(summary = "List card templates")
    @GetMapping
    public List<CardTemplate> list() { return service.findByTenant(TenantContext.get()); }

    @Operation(summary = "Create a card template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PostMapping
    public CardTemplate create(@RequestBody TemplateRequest r) {
        return service.save(CardTemplate.builder().tenantId(TenantContext.get())
                .name(r.name()).logoUrl(r.logoUrl()).primaryColor(r.primaryColor()).secondaryColor(r.secondaryColor())
                .backgroundUrl(r.backgroundUrl()).orientation(r.orientation()).rectoConfig(r.rectoConfig())
                .versoConfig(r.versoConfig()).active(r.active()).build());
    }

    @Operation(summary = "Update a card template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
    @PutMapping("/{id}")
    public CardTemplate update(@PathVariable Long id, @RequestBody TemplateRequest r) {
        return service.update(id, CardTemplate.builder()
                .name(r.name()).logoUrl(r.logoUrl()).primaryColor(r.primaryColor()).secondaryColor(r.secondaryColor())
                .backgroundUrl(r.backgroundUrl()).orientation(r.orientation()).rectoConfig(r.rectoConfig())
                .versoConfig(r.versoConfig()).active(r.active()).build());
    }

    @Operation(summary = "Set a template as default")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    @PatchMapping("/{id}/definir-defaut")
    public CardTemplate setDefault(@PathVariable Long id) { return service.setDefault(TenantContext.get(), id); }

    @Operation(summary = "Delete a card template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
