package gn.lacaeducation.documents.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.StudentBillingProvider;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.grading.StudentReportProvider;
import gn.lacaeducation.payments.StudentPaymentProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Résout automatiquement les variables d'un document à partir des vraies données (école, élève,
 * parent responsable, classe) — l'utilisateur sélectionne le modèle et la personne, le système
 * pré-remplit ; il peut ensuite ajuster manuellement.
 */
@Service
@RequiredArgsConstructor
public class DocumentContextService {

    private final TenantInfoProvider tenantInfo;
    private final StudentDataProvider studentData;
    private final AcademicsDataProvider academics;
    private final TeacherPublicInfoProvider teacherInfo;
    private final StudentReportProvider reportProvider;
    private final StudentBillingProvider billingProvider;
    private final StudentPaymentProvider paymentProvider;

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public Map<String, String> resolve(Long tenantId, String subjectType, Long subjectId) {
        Map<String, String> c = new HashMap<>();
        academics.currentAcademicYearLabel(tenantId).ifPresent(y -> put(c, "annee_scolaire", y));
        tenantInfo.findTenantInfo(tenantId).ifPresent(t -> {
            put(c, "ecole.nom", t.schoolName());
            put(c, "ecole.logo", t.logo());
            put(c, "ecole.adresse", t.address());
            put(c, "ecole.ville", t.city());
            put(c, "ecole.pays", t.country());
            put(c, "ecole.telephone", t.phone());
            put(c, "ecole.email", t.email());
            put(c, "ecole.site", t.website());
            put(c, "ecole.directeur", t.director());
        });
        if ("STUDENT".equalsIgnoreCase(subjectType) && subjectId != null) {
            studentData.findStudent(subjectId).ifPresent(s -> {
                put(c, "eleve.nom", s.lastName());
                put(c, "eleve.prenom", s.firstName());
                put(c, "eleve.matricule", s.studentNumber());
                put(c, "eleve.date_naissance", s.birthDate() != null ? s.birthDate().format(FR) : null);
                put(c, "eleve.lieu_naissance", s.birthPlace());
                put(c, "eleve.sexe", "MALE".equals(s.gender()) ? "Masculin"
                        : "FEMALE".equals(s.gender()) ? "Féminin" : s.gender());
            });
            studentData.findCurrentClassId(subjectId)
                    .flatMap(academics::findClass)
                    .ifPresent(cl -> put(c, "eleve.classe", cl.label()));
            studentData.findParentsByStudent(subjectId).stream().findFirst().ifPresent(p -> {
                put(c, "parent.nom", p.lastName());
                put(c, "parent.prenom", p.firstName());
                put(c, "parent.telephone", p.phone());
            });
            // Bulletin (dernier publié) — pour le document bulletin.
            reportProvider.latestPublished(subjectId).ifPresent(r -> {
                put(c, "bulletin.moyenne", r.average() != null ? r.average().toPlainString() : null);
                put(c, "bulletin.rang", r.rank() != null ? r.rank() + (r.classSize() != null ? "/" + r.classSize() : "") : null);
                put(c, "bulletin.mention", r.mention());
                put(c, "bulletin.decision", r.decision());
            });
            // Par défaut : dernière facture + dernier paiement (peut être remplacé par un choix précis).
            billingProvider.latestInvoice(subjectId).ifPresent(f -> fillInvoice(c, f));
            paymentProvider.latestSucceeded(subjectId).ifPresent(p -> fillPayment(c, p));
        }
        if ("TEACHER".equalsIgnoreCase(subjectType) && subjectId != null) {
            teacherInfo.findTeacher(subjectId).ifPresent(t -> {
                put(c, "enseignant.nom", t.lastName());
                put(c, "enseignant.prenom", t.firstName());
                put(c, "enseignant.matricule", t.staffNumber());
                put(c, "enseignant.specialite", t.specialization());
                put(c, "enseignant.email", t.email());
                put(c, "enseignant.telephone", t.phone());
            });
        }
        // Facture précise (document facture / échéancier).
        if ("INVOICE".equalsIgnoreCase(subjectType) && subjectId != null) {
            billingProvider.findInvoice(subjectId).ifPresent(f -> {
                fillInvoice(c, f);
                if (f.studentId() != null) fillStudentBasics(c, f.studentId());
            });
        }
        // Paiement précis (reçu).
        if ("PAYMENT".equalsIgnoreCase(subjectType) && subjectId != null) {
            paymentProvider.findPayment(subjectId).ifPresent(p -> {
                fillPayment(c, p);
                if (p.invoiceId() != null) billingProvider.findInvoice(p.invoiceId()).ifPresent(f -> fillInvoice(c, f));
                if (p.studentId() != null) fillStudentBasics(c, p.studentId());
            });
        }
        return c;
    }

    private void fillInvoice(Map<String, String> c, StudentBillingProvider.InvoiceInfo f) {
        put(c, "facture.reference", f.reference());
        put(c, "facture.total", money(f.total()));
        put(c, "facture.paye", money(f.paid()));
        put(c, "facture.reste", money(f.remaining()));
        put(c, "facture.statut", f.status());
        put(c, "facture.periode", f.periodLabel());
    }

    private void fillPayment(Map<String, String> c, StudentPaymentProvider.PaymentInfo p) {
        put(c, "paiement.reference", p.reference());
        put(c, "paiement.montant", money(p.amount()));
        put(c, "paiement.mode", channelLabel(p.channel()));
        put(c, "paiement.date", p.date() != null ? p.date().toLocalDate().format(FR) : null);
    }

    /** Remplit les variables élève de base (nom, prénom, matricule, classe) pour facture/reçu. */
    private void fillStudentBasics(Map<String, String> c, Long studentId) {
        studentData.findStudent(studentId).ifPresent(s -> {
            put(c, "eleve.nom", s.lastName());
            put(c, "eleve.prenom", s.firstName());
            put(c, "eleve.matricule", s.studentNumber());
        });
        studentData.findCurrentClassId(studentId).flatMap(academics::findClass)
                .ifPresent(cl -> put(c, "eleve.classe", cl.label()));
    }

    private void put(Map<String, String> c, String key, String value) {
        if (value != null && !value.isBlank()) c.put(key, value);
    }

    private static String money(java.math.BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : null;
    }

    private static String channelLabel(String channel) {
        if (channel == null) return null;
        return switch (channel) {
            case "ORANGE_MONEY" -> "Orange Money";
            case "MTN_MONEY" -> "MTN Money";
            case "WAVE" -> "Wave";
            case "CARTE_BANCAIRE", "STRIPE" -> "Carte bancaire";
            case "DJOMY" -> "Djomy";
            case "CASH", "ESPECES" -> "Espèces";
            default -> channel.replace('_', ' ');
        };
    }
}
