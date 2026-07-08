package gn.lacaeducation.documents.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.SchoolCode;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.documents.application.port.out.DocumentTemplateRepository;
import gn.lacaeducation.documents.application.port.out.GeneratedDocumentRepository;
import gn.lacaeducation.documents.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentTemplateRepository templateRepo;
    private final GeneratedDocumentRepository generatedRepo;
    private final DocumentRenderService renderer;
    private final TenantInfoProvider tenantInfoProvider;

    public record TemplateCommand(Long tenantId, DocumentCategory category, DocumentType documentType, String name,
                                  DocumentStyle style, String headerHtml, String bodyHtml, String footerHtml,
                                  String primaryColor, String fontFamily, String logoUrl, String signatureUrl,
                                  String stampUrl, String watermarkText, PageFormat pageFormat,
                                  Integer marginTop, Integer marginRight, Integer marginBottom, Integer marginLeft,
                                  String numberingPrefix, Boolean active) {}
    public record RenderResult(String html, String reference) {}

    // ── Modèles ─────────────────────────────────────────────────────────────────

    public DocumentTemplate create(TemplateCommand c) {
        if (c.name() == null || c.name().isBlank()) throw new BusinessRuleException("Le nom du modèle est requis");
        if (c.documentType() == null) throw new BusinessRuleException("Le type de document est requis");
        DocumentTemplate t = apply(DocumentTemplate.builder()
                .tenantId(c.tenantId()).version(1).isDefault(Boolean.FALSE)
                .createdAt(LocalDateTime.now()).build(), c);
        return templateRepo.save(t);
    }

    public DocumentTemplate update(Long id, TemplateCommand c) {
        DocumentTemplate t = load(id);
        apply(t, c);
        t.setVersion((t.getVersion() != null ? t.getVersion() : 1) + 1);
        t.setUpdatedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    private DocumentTemplate apply(DocumentTemplate t, TemplateCommand c) {
        t.setCategory(c.category() != null ? c.category() : DocumentCategory.ADMINISTRATIF);
        t.setDocumentType(c.documentType());
        t.setName(c.name());
        t.setStyle(c.style() != null ? c.style() : DocumentStyle.MODERNE);
        t.setHeaderHtml(c.headerHtml()); t.setBodyHtml(c.bodyHtml()); t.setFooterHtml(c.footerHtml());
        t.setPrimaryColor(c.primaryColor()); t.setFontFamily(c.fontFamily());
        t.setLogoUrl(c.logoUrl()); t.setSignatureUrl(c.signatureUrl()); t.setStampUrl(c.stampUrl());
        t.setWatermarkText(c.watermarkText());
        t.setPageFormat(c.pageFormat() != null ? c.pageFormat() : PageFormat.A4);
        t.setMarginTop(c.marginTop() != null ? c.marginTop() : 18);
        t.setMarginRight(c.marginRight() != null ? c.marginRight() : 16);
        t.setMarginBottom(c.marginBottom() != null ? c.marginBottom() : 18);
        t.setMarginLeft(c.marginLeft() != null ? c.marginLeft() : 16);
        t.setNumberingPrefix(c.numberingPrefix());
        t.setActive(c.active() == null || c.active());
        if (t.getIsDefault() == null) t.setIsDefault(Boolean.FALSE);
        return t;
    }

    public DocumentTemplate setDefault(Long id) {
        DocumentTemplate t = load(id);
        for (DocumentTemplate other : templateRepo.findByTenantIdAndType(t.getTenantId(), t.getDocumentType())) {
            if (!other.getId().equals(id) && Boolean.TRUE.equals(other.getIsDefault())) {
                other.setIsDefault(Boolean.FALSE);
                templateRepo.save(other);
            }
        }
        t.setIsDefault(Boolean.TRUE);
        return templateRepo.save(t);
    }

    public void delete(Long id) { templateRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<DocumentTemplate> templates(Long tenantId) { return templateRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public DocumentTemplate template(Long id) { return load(id); }

    // ── Génération ──────────────────────────────────────────────────────────────

    public RenderResult generate(Long tenantId, Long templateId, Map<String, String> context,
                                 String subjectType, Long subjectId, String title, Long userId) {
        DocumentTemplate t = load(templateId);
        long seq = generatedRepo.countByTenantIdAndType(tenantId, t.getDocumentType()) + 1;
        String sigle = SchoolCode.prefix(tenantInfoProvider, tenantId);
        String typePrefix = t.getNumberingPrefix() != null && !t.getNumberingPrefix().isBlank()
                ? t.getNumberingPrefix() : t.getDocumentType().name();
        // Sigle de l'école en tête, suivi d'une codification lisible (type-année-séquence).
        String reference = String.format("%s-%s-%s-%06d", sigle, typePrefix, Year.now(), seq);

        Map<String, String> ctx = new java.util.HashMap<>();
        if (context != null) ctx.putAll(context);
        ctx.putIfAbsent("reference", reference);
        String html = renderer.render(t, ctx);

        generatedRepo.save(GeneratedDocument.builder()
                .tenantId(tenantId).templateId(templateId).documentType(t.getDocumentType()).reference(reference)
                .title(title != null ? title : t.getName()).subjectType(subjectType).subjectId(subjectId)
                .generatedBy(userId).generatedAt(LocalDateTime.now()).build());
        return new RenderResult(html, reference);
    }

    /** Prévisualisation sans numéroter ni historiser (édition de modèle). */
    @Transactional(readOnly = true)
    public String preview(Long templateId, Map<String, String> context) {
        return renderer.render(load(templateId), context);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocument> history(Long tenantId) { return generatedRepo.findByTenantId(tenantId); }

    // ── Modèles prédéfinis (installation initiale) ────────────────────────────────

    // En-tête en bandeau coloré (logo + école à gauche, référence/date à droite).
    private static final String HEADER =
            "<div style=\"background:{{couleur}};color:#fff;padding:16px 20px;border-radius:12px\">"
            + "<table style=\"width:100%;border-collapse:collapse\"><tr>"
            + "<td style=\"border:none;width:74px;vertical-align:middle\">{{logo}}</td>"
            + "<td style=\"border:none;vertical-align:middle\"><div style=\"font-size:19px;font-weight:800\">{{ecole.nom}}</div>"
            + "<div style=\"font-size:11px;opacity:.85\">{{ecole.adresse}} · {{ecole.telephone}}</div></td>"
            + "<td style=\"border:none;text-align:right;vertical-align:middle;font-size:11px;opacity:.92\">Réf. {{reference}}<br>{{date}}</td>"
            + "</tr></table></div>";
    private static final String FOOTER =
            "<div style=\"text-align:center;color:#94a3b8;font-size:10px\">{{ecole.nom}} · {{ecole.adresse}} · {{ecole.telephone}} · {{ecole.email}}</div>";
    private static final String SIGN =
            "<div style=\"margin-top:44px;text-align:right\"><div style=\"display:inline-block;text-align:center\">"
            + "<div style=\"color:#64748b;font-size:12px;margin-bottom:6px\">Le Directeur</div>"
            + "<div>{{signature}}</div><div style=\"margin-top:2px\">{{cachet}}</div></div></div>";

    /** Titre centré avec trait d'accent (style moderne). */
    private static String title(String t) {
        return "<h1 style=\"text-align:center;letter-spacing:1px;margin:30px 0 4px;font-size:22px\">" + t + "</h1>"
                + "<div style=\"width:84px;height:3px;background:{{couleur}};margin:2px auto 26px;border-radius:2px\"></div>";
    }
    /** Bloc de contenu encadré (carte claire). */
    private static String card(String inner) {
        return "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:18px 22px;line-height:1.85;font-size:14px\">"
                + inner + "</div>";
    }

    /** Installe une bibliothèque de modèles professionnels prêts à l'emploi (si le tenant n'en a pas). */
    public List<DocumentTemplate> seedDefaults(Long tenantId) {
        if (!templateRepo.findByTenantId(tenantId).isEmpty()) {
            throw new BusinessRuleException("Des modèles existent déjà — supprimez-les avant de réinstaller les modèles par défaut.");
        }
        List<DocumentTemplate> out = new java.util.ArrayList<>();
        out.add(seed(tenantId, DocumentCategory.ELEVE, DocumentType.CERTIFICAT_SCOLARITE, "Certificat de scolarité", "CERT",
                title("CERTIFICAT DE SCOLARITÉ")
                + card("Je soussigné(e), Directeur de l'établissement <strong>{{ecole.nom}}</strong>, certifie que l'élève "
                + "<strong>{{eleve.prenom}} {{eleve.nom}}</strong>, né(e) le {{eleve.date_naissance}} à {{eleve.lieu_naissance}}, "
                + "matricule <strong>{{eleve.matricule}}</strong>, est régulièrement inscrit(e) en classe de "
                + "<strong>{{eleve.classe}}</strong> au titre de l'année scolaire <strong>{{annee_scolaire}}</strong>."
                + "<br><br>Le présent certificat est délivré pour servir et valoir ce que de droit.")
                + "<p style=\"margin-top:22px\">Fait à {{ecole.ville}}, le {{date}}.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.ELEVE, DocumentType.ATTESTATION_INSCRIPTION, "Attestation d'inscription", "INSC",
                title("ATTESTATION D'INSCRIPTION")
                + card("L'établissement <strong>{{ecole.nom}}</strong> atteste que <strong>{{eleve.prenom}} {{eleve.nom}}</strong> "
                + "(matricule {{eleve.matricule}}) est inscrit(e) en classe de <strong>{{eleve.classe}}</strong> "
                + "pour l'année scolaire <strong>{{annee_scolaire}}</strong>.")
                + "<p style=\"margin-top:22px\">Fait à {{ecole.ville}}, le {{date}}.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.FINANCIER, DocumentType.RECU_PAIEMENT, "Reçu de paiement", "RECU",
                title("REÇU DE PAIEMENT")
                + "<div style=\"text-align:center;margin:4px 0 22px\"><span style=\"display:inline-block;background:{{couleur}};color:#fff;"
                + "padding:12px 30px;border-radius:12px;font-size:22px;font-weight:800\">{{paiement.montant}} GNF</span></div>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:13px\">"
                + "<tr><th style=\"text-align:left;background:#f1f5f9;width:160px\">Élève</th><td>{{eleve.prenom}} {{eleve.nom}} — {{eleve.classe}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Mode de paiement</th><td>{{paiement.mode}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Date</th><td>{{paiement.date}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Référence</th><td>{{reference}}</td></tr></table>"
                + "<p style=\"margin-top:16px;color:#475569\">Reçu la somme indiquée ci-dessus, dont quittance.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.ADMINISTRATIF, DocumentType.CONVOCATION, "Convocation", "CONV",
                title("CONVOCATION")
                + card("Madame, Monsieur le parent de l'élève <strong>{{eleve.prenom}} {{eleve.nom}}</strong> "
                + "(classe {{eleve.classe}}), vous êtes prié(e) de bien vouloir vous présenter à l'établissement <strong>{{ecole.nom}}</strong>."
                + "<br><br><strong>Motif :</strong> {{motif}}<br><strong>Date &amp; heure :</strong> {{rendez_vous}}")
                + "<p style=\"margin-top:22px\">Fait à {{ecole.ville}}, le {{date}}.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.RH, DocumentType.ATTESTATION_TRAVAIL, "Attestation de travail", "ATT-TRAV",
                title("ATTESTATION DE TRAVAIL")
                + card("Je soussigné(e), Directeur de <strong>{{ecole.nom}}</strong>, atteste que "
                + "<strong>{{enseignant.prenom}} {{enseignant.nom}}</strong> (matricule {{enseignant.matricule}}) exerce au sein de "
                + "notre établissement en qualité d'enseignant(e) de <strong>{{enseignant.specialite}}</strong>."
                + "<br><br>La présente attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.")
                + "<p style=\"margin-top:22px\">Fait à {{ecole.ville}}, le {{date}}.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.ELEVE, DocumentType.BULLETIN, "Bulletin de notes (synthèse)", "BUL",
                title("BULLETIN DE NOTES")
                + card("Élève : <strong>{{eleve.prenom}} {{eleve.nom}}</strong> — Matricule {{eleve.matricule}}<br>"
                + "Classe : <strong>{{eleve.classe}}</strong> · Période : <strong>{{periode}}</strong> · Année : {{annee_scolaire}}")
                + "<table style=\"width:100%;margin-top:16px;border-collapse:collapse;font-size:13px\">"
                + "<tr><th style=\"text-align:left;background:#f1f5f9;width:200px\">Moyenne générale</th><td><strong>{{bulletin.moyenne}} / 20</strong></td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Rang</th><td>{{bulletin.rang}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Mention</th><td>{{bulletin.mention}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Décision</th><td>{{bulletin.decision}}</td></tr></table>"
                + "<p style=\"margin-top:14px;color:#475569\">Appréciation : {{appreciation}}</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.RH, DocumentType.FICHE_PAIE, "Fiche de paie", "PAIE",
                title("FICHE DE PAIE")
                + card("<strong>{{enseignant.prenom}} {{enseignant.nom}}</strong> — Matricule {{enseignant.matricule}}<br>Période : <strong>{{paie.periode}}</strong>")
                + "<div style=\"text-align:center;margin:6px 0 18px\"><span style=\"display:inline-block;background:{{couleur}};color:#fff;"
                + "padding:12px 28px;border-radius:12px;font-size:20px;font-weight:800\">Net : {{paie.net}} GNF</span></div>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:13px\">"
                + "<tr><th style=\"text-align:left;background:#f1f5f9;width:200px\">Heures effectives</th><td>{{paie.heures}}</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Salaire brut</th><td>{{paie.brut}} GNF</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Retenues</th><td>{{paie.retenues}} GNF</td></tr>"
                + "<tr><th style=\"text-align:left;background:#f1f5f9\">Net à payer</th><td><strong>{{paie.net}} GNF</strong></td></tr></table>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.ELEVE, DocumentType.DIPLOME, "Diplôme", "DIP",
                title("DIPLÔME")
                + "<p style=\"text-align:center;color:#64748b\">Décerné à</p>"
                + "<h2 style=\"text-align:center;margin:4px 0 18px\">{{eleve.prenom}} {{eleve.nom}}</h2>"
                + "<div style=\"border:2px solid {{couleur}};border-radius:14px;padding:24px;text-align:center;line-height:1.85\">"
                + "Pour avoir satisfait aux exigences de <strong>{{diplome.intitule}}</strong> avec la mention "
                + "<strong>{{diplome.mention}}</strong>, au titre de l'année scolaire <strong>{{annee_scolaire}}</strong>.</div>"
                + "<p style=\"margin-top:20px;text-align:center\">Fait à {{ecole.ville}}, le {{date}}.</p>" + SIGN));
        out.add(seed(tenantId, DocumentCategory.PEDAGOGIQUE, DocumentType.EMPLOI_DU_TEMPS, "Emploi du temps (couverture)", "EDT",
                title("EMPLOI DU TEMPS")
                + card("Classe : <strong>{{eleve.classe}}</strong> · Année scolaire : <strong>{{annee_scolaire}}</strong><br><br>"
                + "Le présent emploi du temps est applicable à compter du {{date}}. La grille hebdomadaire détaillée "
                + "est éditée depuis le module « Emploi du temps ».")));
        out.add(seed(tenantId, DocumentCategory.RH, DocumentType.CONTRAT_ENSEIGNANT, "Contrat enseignant", "CONTRAT",
                title("CONTRAT DE TRAVAIL")
                + card("Entre l'établissement <strong>{{ecole.nom}}</strong> (l'Employeur) et "
                + "<strong>{{enseignant.prenom}} {{enseignant.nom}}</strong> (le Salarié), matricule {{enseignant.matricule}}, "
                + "il est convenu ce qui suit :<br><br><strong>Poste :</strong> Enseignant(e) de {{enseignant.specialite}}<br>"
                + "<strong>Date de prise de fonction :</strong> {{contrat.debut}}<br><strong>Rémunération :</strong> {{contrat.remuneration}}")
                + "<p style=\"margin-top:16px\">Fait à {{ecole.ville}}, le {{date}}, en deux exemplaires.</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin-top:34px\"><tr>"
                + "<td style=\"border:none;text-align:center;color:#64748b;font-size:12px\">L'Employeur</td>"
                + "<td style=\"border:none;text-align:center;color:#64748b;font-size:12px\">Le Salarié</td></tr></table>"));
        return out;
    }

    private DocumentTemplate seed(Long tenantId, DocumentCategory cat, DocumentType type, String name, String prefix, String body) {
        DocumentTemplate t = create(new TemplateCommand(tenantId, cat, type, name, DocumentStyle.PREMIUM,
                HEADER, body, FOOTER, "#1d4ed8", "system-ui, 'Segoe UI', Arial, sans-serif",
                null, null, null, null, PageFormat.A4, 16, 18, 16, 18, prefix, true));
        return setDefault(t.getId());
    }

    private DocumentTemplate load(Long id) {
        return templateRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("DocumentTemplate", id));
    }
}
