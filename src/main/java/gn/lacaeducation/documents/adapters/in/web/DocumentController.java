package gn.lacaeducation.documents.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.documents.application.service.DocumentService;
import gn.lacaeducation.documents.application.service.DocumentService.RenderResult;
import gn.lacaeducation.documents.application.service.DocumentService.TemplateCommand;
import gn.lacaeducation.documents.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Documents", description = "Documents & Modèles : moteur de templates personnalisables, génération, historique")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;
    private final gn.lacaeducation.documents.application.service.DocumentContextService contextService;

    public record TemplateRequest(DocumentCategory category, DocumentType documentType, String name,
                                  DocumentStyle style, String headerHtml, String bodyHtml, String footerHtml,
                                  String primaryColor, String fontFamily, String logoUrl, String signatureUrl,
                                  String stampUrl, String watermarkText, PageFormat pageFormat,
                                  Integer marginTop, Integer marginRight, Integer marginBottom, Integer marginLeft,
                                  String numberingPrefix, Boolean active) {}
    public record GenerateRequest(Map<String, String> context, String subjectType, Long subjectId, String title) {}
    public record PreviewRequest(Map<String, String> context) {}

    // ── Modèles ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Lister les modèles du tenant")
    @GetMapping("/templates")
    public List<DocumentTemplate> templates() { return service.templates(TenantContext.get()); }

    @Operation(summary = "Détail d'un modèle")
    @GetMapping("/templates/{id}")
    public DocumentTemplate template(@PathVariable Long id) { return service.template(id); }

    @Operation(summary = "Créer un modèle")
    @PreAuthorize("hasAuthority('document:manage')")
    @PostMapping("/templates")
    public DocumentTemplate create(@RequestBody TemplateRequest r) {
        return service.create(toCommand(r));
    }

    @Operation(summary = "Installer la bibliothèque de modèles prédéfinis (professionnels, prêts à l'emploi)")
    @PreAuthorize("hasAuthority('document:manage')")
    @PostMapping("/templates/seed-defaults")
    public List<DocumentTemplate> seedDefaults() { return service.seedDefaults(TenantContext.get()); }

    @Operation(summary = "Modifier un modèle (nouvelle version)")
    @PreAuthorize("hasAuthority('document:manage')")
    @PutMapping("/templates/{id}")
    public DocumentTemplate update(@PathVariable Long id, @RequestBody TemplateRequest r) {
        return service.update(id, toCommand(r));
    }

    @Operation(summary = "Définir le modèle par défaut de son type")
    @PreAuthorize("hasAuthority('document:manage')")
    @PatchMapping("/templates/{id}/default")
    public DocumentTemplate setDefault(@PathVariable Long id) { return service.setDefault(id); }

    @Operation(summary = "Supprimer un modèle")
    @PreAuthorize("hasAuthority('document:manage')")
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Prévisualiser un modèle avec un contexte (sans numéroter)")
    @PreAuthorize("hasAuthority('document:generate')")
    @PostMapping("/templates/{id}/preview")
    public Map<String, String> preview(@PathVariable Long id, @RequestBody PreviewRequest r) {
        return Map.of("html", service.preview(id, r.context()));
    }

    @Operation(summary = "Générer un document (numéroté + historisé) à partir d'un modèle")
    @PreAuthorize("hasAuthority('document:generate')")
    @PostMapping("/templates/{id}/generate")
    public RenderResult generate(@PathVariable Long id, @RequestBody GenerateRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.generate(TenantContext.get(), id, r.context(), r.subjectType(), r.subjectId(), r.title(), currentUserId(jwt));
    }

    // ── Historique & métadonnées ─────────────────────────────────────────────────

    @Operation(summary = "Historique des documents générés")
    @GetMapping("/history")
    public List<GeneratedDocument> history() { return service.history(TenantContext.get()); }

    @Operation(summary = "Résout automatiquement les variables d'un sujet (école + élève réels)")
    @GetMapping("/context")
    public Map<String, String> context(@RequestParam(required = false) String subjectType, @RequestParam(required = false) Long subjectId) {
        return contextService.resolve(TenantContext.get(), subjectType, subjectId);
    }

    @Operation(summary = "Catalogue des variables disponibles dans les modèles")
    @GetMapping("/variables")
    public List<Map<String, Object>> variables() {
        return List.of(
                group("École", "ecole.nom", "ecole.adresse", "ecole.telephone", "ecole.email", "ecole.ville", "logo", "cachet", "signature", "watermark"),
                group("Document", "date", "reference", "annee_scolaire"),
                group("Élève", "eleve.nom", "eleve.prenom", "eleve.matricule", "eleve.classe", "eleve.date_naissance", "eleve.lieu_naissance", "eleve.sexe"),
                group("Parent", "parent.nom", "parent.prenom", "parent.telephone"),
                group("Enseignant", "enseignant.nom", "enseignant.prenom", "enseignant.matricule", "enseignant.specialite"),
                group("Finance", "paiement.montant", "paiement.mode", "paiement.date", "facture.reference", "facture.total", "facture.reste"),
                group("Notes", "bulletin.moyenne", "bulletin.rang", "bulletin.mention", "bulletin.decision")
        );
    }

    private Map<String, Object> group(String name, String... vars) {
        return Map.of("group", name, "variables", List.of(vars));
    }

    private TemplateCommand toCommand(TemplateRequest r) {
        return new TemplateCommand(TenantContext.get(), r.category(), r.documentType(), r.name(), r.style(),
                r.headerHtml(), r.bodyHtml(), r.footerHtml(), r.primaryColor(), r.fontFamily(), r.logoUrl(),
                r.signatureUrl(), r.stampUrl(), r.watermarkText(), r.pageFormat(), r.marginTop(), r.marginRight(),
                r.marginBottom(), r.marginLeft(), r.numberingPrefix(), r.active());
    }

    private Long currentUserId(Jwt jwt) {
        try { return jwt == null ? null : Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
