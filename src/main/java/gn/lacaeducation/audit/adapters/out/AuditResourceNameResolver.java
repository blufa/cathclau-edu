package gn.lacaeducation.audit.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.audit.adapters.in.web.dto.AuditLogResponse;
import gn.lacaeducation.audit.domain.AuditLog;
import gn.lacaeducation.billing.InvoiceDataProvider;
import gn.lacaeducation.billing.InvoicePublicInfo;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.PaymentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Résout l'acteur et la ressource d'une entrée de journal d'audit en NOMS lisibles (§retour comptable :
 * "Facture #60", "Échéance #412", "Inscription #14" ne permettent pas de repérer quoi que ce soit) —
 * centralisé côté backend pour couvrir toutes les ressources d'un coup, sans dépendre côté frontend de
 * permissions annexes ({@code user:read} pour nommer un acteur, par exemple) que le rôle consultant le
 * journal n'a pas forcément. Résolution au cas par cas, jamais nulle en cas d'échec (le frontend retombe
 * sur "Ressource #id" si {@code ressourceNom} est absent).
 */
@Component
@RequiredArgsConstructor
public class AuditResourceNameResolver {

    private final UserPublicInfoProvider userProvider;
    private final StudentDataProvider studentProvider;
    private final InvoiceDataProvider invoiceDataProvider;
    private final PaymentDataProvider paymentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;

    public AuditLogResponse toResponse(AuditLog log) {
        String utilisateurNom = log.getUtilisateurId() == null ? null : userName(log.getUtilisateurId());
        String ressourceNom = resolveResourceName(log);
        return new AuditLogResponse(log.getId(), log.getTenantId(), log.getUtilisateurId(), utilisateurNom,
                log.getAction(), log.getRessource(), log.getRessourceId(), ressourceNom, log.getDetails(), log.getCreatedAt());
    }

    private String resolveResourceName(AuditLog log) {
        Long id = log.getRessourceId();
        if (id == null || log.getRessource() == null) return null;
        return switch (log.getRessource()) {
            case "User" -> userName(id);
            case "Student" -> studentProvider.findStudent(id).map(s -> fullName(s.firstName(), s.lastName())).orElse(null);
            case "Invoice" -> invoiceDataProvider.findInvoice(id).map(InvoicePublicInfo::reference).orElse(null);
            case "Installment" -> invoiceDataProvider.findInstallmentLabel(id).orElse(null);
            case "SchoolFee" -> invoiceDataProvider.findSchoolFeeLabel(id).orElse(null);
            case "Payment" -> paymentDataProvider.findPayment(id).map(PaymentPublicInfo::reference).orElse(null);
            case "Classe" -> academicsDataProvider.findClass(id).map(c -> c.label()).orElse(null);
            // L'inscription elle-même n'a pas de nom propre — on résout le nom de l'ÉLÈVE concerné,
            // porté dans le détail ("studentId=…") faute d'API de lecture dédiée aux inscriptions.
            case "Enrollment" -> resolveEnrollmentStudentName(log.getDetails());
            default -> null;
        };
    }

    private String resolveEnrollmentStudentName(String details) {
        Long studentId = parseDetailLong(details, "studentId");
        if (studentId == null) return null;
        return studentProvider.findStudent(studentId).map(s -> fullName(s.firstName(), s.lastName())).orElse(null);
    }

    private String userName(Long userId) {
        return userProvider.findUser(userId)
                .map(u -> fullName(u.firstName(), u.lastName()))
                .orElse(null);
    }

    private static Long parseDetailLong(String details, String key) {
        if (details == null) return null;
        for (String part : details.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return Long.valueOf(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String fullName(String first, String last) {
        String n = (safe(first) + " " + safe(last)).trim();
        return n.isBlank() ? null : n;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
