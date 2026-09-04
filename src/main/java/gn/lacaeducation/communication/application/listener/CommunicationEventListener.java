package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.billing.application.event.InvoicePaymentAppliedEvent;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.*;
import gn.lacaeducation.grading.application.event.AssessmentScheduledEvent;
import gn.lacaeducation.grading.application.event.GradeModifiedEvent;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns domain events into outbound SMS/Email/WhatsApp communications to the student's parents (§11.1).
 * Each notification is system-generated (no human sender) and addressed by the parent's resolved
 * contact (phone/email), so the recipient may have no user account. When the tenant has configured a
 * {@link MessageTemplate} for the reason, its {@code {{variable}}} body is rendered; otherwise a
 * sensible French default is used.
 */
@Component @RequiredArgsConstructor @Slf4j
public class CommunicationEventListener {

    private final CommunicationUseCase communicationUseCase;
    private final MessageTemplateRepository templateRepository;
    private final StudentDataProvider studentDataProvider;
    private final PaymentDataProvider paymentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final TenantInfoProvider tenantInfoProvider;
    private final gn.lacaeducation.iam.SchoolStaffDirectory schoolStaffDirectory;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Alerte par email, à chaque modification de note par un enseignant, l'administration de l'école, le
     * responsable pédagogique <em>et</em> le(s) parent(s) de l'élève, en précisant l'ancienne/nouvelle
     * valeur et le motif obligatoire (§7.2).
     */
    @TransactionalEventListener
    public void onGradeModified(GradeModifiedEvent event) {
        String studentName = studentDataProvider.findStudent(event.studentId())
                .map(this::fullName).orElse("l'élève #" + event.studentId());
        String subject = "Modification d'une note";
        String body = "La note de " + studentName + " a été modifiée de " + event.oldValue()
                + " à " + event.newValue() + ". Motif : " + event.reason() + ".";

        // 1) Administration + responsable pédagogique (groupe interne).
        List<RecipientTarget> staffTargets = schoolStaffDirectory.findGradeChangeRecipients(event.tenantId()).stream()
                .filter(c -> c.email() != null && !c.email().isBlank())
                .map(c -> new RecipientTarget(null, c.email()))
                .toList();
        if (staffTargets.isEmpty()) {
            log.debug("Aucun destinataire (admin/responsable pédagogique) pour la modification de note {}",
                    event.gradeId());
        } else {
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), event.changedBy(),
                    CommunicationChannel.EMAIL, RecipientType.GROUP, subject, body,
                    CommunicationReason.GENERAL, staffTargets));
        }

        // 2) Parent(s) de l'élève par email.
        List<RecipientTarget> parentTargets = studentDataProvider.findParentsByStudent(event.studentId()).stream()
                .map(ParentPublicInfo::email)
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (parentTargets.isEmpty()) {
            log.debug("Aucun parent joignable par email pour la modification de note {}", event.gradeId());
        } else {
            communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), event.changedBy(),
                    CommunicationChannel.EMAIL, RecipientType.PARENT, subject, body,
                    CommunicationReason.GENERAL, parentTargets));
        }
    }

    @TransactionalEventListener
    public void onStudentAbsent(StudentAbsentEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String className = academicsDataProvider.findClass(event.classId()).map(c -> c.label()).orElse("N/A");
            String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                    "classe", className, "date", date);
            String defaultBody = "Votre enfant " + fullName(student) + " a été absent(e) le " + date
                    + " en classe de " + className + ".";
            dispatch(event.tenantId(), CommunicationReason.ABSENCE, "Absence signalée", defaultBody, vars, event.studentId());
        });
    }

    @TransactionalEventListener
    public void onReportCardPublished(ReportCardPublishedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String period = academicsDataProvider.findPeriod(event.periodId()).map(p -> p.label()).orElse("N/A");
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()), "periode", period);
            String defaultBody = "Le bulletin de " + fullName(student) + " pour " + period
                    + " est désormais disponible.";
            dispatch(event.tenantId(), CommunicationReason.RESULT, "Bulletin disponible", defaultBody, vars, event.studentId());
        });
    }

    @TransactionalEventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        // Invoice-linked payments are handled by onInvoicePaymentApplied (which knows the remaining
        // balance). Here we only cover payments not tied to a school-fee invoice, to avoid duplicates.
        if (event.invoiceId() != null) return;
        studentDataProvider.findStudent(event.studentId()).ifPresent(student ->
            paymentDataProvider.findPayment(event.paymentId()).ifPresent(payment -> {
                String amount = payment.amount() + " FCFA";
                Map<String, String> vars = Map.of(
                        "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                        "montant", amount, "canal", payment.channel() == null ? "" : payment.channel(),
                        "reference", payment.reference() == null ? "" : payment.reference());
                String defaultBody = "Paiement de " + amount + " reçu pour " + fullName(student) + ". Merci.";
                dispatch(event.tenantId(), CommunicationReason.GENERAL, "Paiement reçu", defaultBody, vars, event.studentId());
                notifyFounder(event.tenantId(), fullName(student), amount);
            }));
    }

    /** Notify the parent of each payment applied to a school-fee invoice, stating the remaining balance (§11.1). */
    @TransactionalEventListener
    public void onInvoicePaymentApplied(InvoicePaymentAppliedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String amount = event.paidAmount() + " FCFA";
            BigDecimal remaining = event.remainingBalance() == null ? BigDecimal.ZERO : event.remainingBalance();
            boolean settled = remaining.compareTo(BigDecimal.ZERO) <= 0;
            String remainingStr = remaining + " FCFA";
            Map<String, String> vars = Map.of(
                    "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                    "montant", amount, "reste_a_payer", remainingStr,
                    "reference", event.reference() == null ? "" : event.reference());
            String defaultBody = settled
                    ? "Paiement de " + amount + " reçu pour " + fullName(student) + ". Facture soldée. Merci."
                    : "Paiement de " + amount + " reçu pour " + fullName(student) + ". Reste à payer : " + remainingStr + ".";
            dispatch(event.tenantId(), CommunicationReason.GENERAL, "Paiement reçu", defaultBody, vars, event.studentId());
            notifyFounder(event.tenantId(), fullName(student), amount);
        });
    }

    /** Notify every (non-cancelled) student of the class on their own contact when an assessment is scheduled. */
    @TransactionalEventListener
    public void onAssessmentScheduled(AssessmentScheduledEvent event) {
        List<StudentPublicInfo> students = studentDataProvider.findStudentsByClass(event.classId());
        if (students.isEmpty()) {
            log.debug("No student in class {}; skipping assessment notification", event.classId());
            return;
        }
        String className = academicsDataProvider.findClass(event.classId()).map(c -> c.label()).orElse("N/A");
        String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
        String label = (event.label() != null && !event.label().isBlank()) ? event.label() : "une évaluation";
        Map<String, String> vars = Map.of(
                "ecole", schoolName(event.tenantId()), "classe", className, "evaluation", label, "date", date);
        String defaultBody = "Une évaluation (" + label + ") est programmée le " + date
                + " pour la classe " + className + ".";

        Resolved r = resolve(event.tenantId(), CommunicationReason.EXAM, "Évaluation programmée", defaultBody, vars);
        List<RecipientTarget> targets = students.stream()
                .map(s -> studentDestination(s, r.channel()))
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (targets.isEmpty()) {
            log.debug("No reachable student in class {} on channel {}; skipping assessment notification",
                    event.classId(), r.channel());
            return;
        }
        communicationUseCase.send(new SendCommunicationCommand(event.tenantId(), null, r.channel(),
                RecipientType.STUDENT, r.subject(), r.content(), CommunicationReason.EXAM, targets));
    }

    // ── dispatch ───────────────────────────────────────────────────────────────
    /** Resolved channel/subject/content after applying the tenant template (or French defaults). */
    private record Resolved(CommunicationChannel channel, String subject, String content) {}

    private Resolved resolve(Long tenantId, CommunicationReason reason, String defaultSubject,
                             String defaultBody, Map<String, String> vars) {
        String school = schoolName(tenantId);
        // Variable {{ecole}} disponible pour les modèles, sans toucher aux appelants.
        Map<String, String> allVars = new HashMap<>(vars == null ? Map.of() : vars);
        allVars.putIfAbsent("ecole", school);

        Optional<MessageTemplate> template = templateRepository.findByTenantIdAndReason(tenantId, reason);
        CommunicationChannel channel = template.map(MessageTemplate::getChannel).orElse(CommunicationChannel.SMS);
        if (channel == null) channel = CommunicationChannel.SMS;
        String subject = template.map(t -> MessageTemplate.render(t.getSubject(), allVars)).orElse(defaultSubject);
        String content = template.map(t -> MessageTemplate.render(t.getBody(), allVars)).orElse(defaultBody);

        // SMS : le sender Orange global est « CathClau » ; on préfixe le nom de l'école pour que le parent
        // identifie l'expéditeur. Pas de doublon si le contenu contient déjà le nom (ex. modèle {{ecole}}).
        if (channel == CommunicationChannel.SMS && content != null && !content.contains(school)) {
            content = "[" + school + "] " + content;
        }
        return new Resolved(channel, subject, content);
    }

    private void dispatch(Long tenantId, CommunicationReason reason, String defaultSubject,
                          String defaultBody, Map<String, String> vars, Long studentId) {
        Resolved r = resolve(tenantId, reason, defaultSubject, defaultBody, vars);
        List<RecipientTarget> targets = studentDataProvider.findParentsByStudent(studentId).stream()
                .map(parent -> destinationFor(parent, r.channel()))
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (targets.isEmpty()) {
            log.debug("No reachable parent for student {} on channel {}; skipping {} notification", studentId, r.channel(), reason);
            return;
        }
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, r.channel(),
                RecipientType.PARENT, r.subject(), r.content(), reason, targets));
    }

    /**
     * Alerte par SMS le(s) compte(s) SCHOOL_ADMIN de l'école (« fondateur ») dès qu'un paiement est
     * reçu, en plus de la notification au parent — décision produit : pas encore de numéro de
     * fondateur dédié dans les paramètres du tenant, on utilise le compte administrateur (§communication).
     */
    private void notifyFounder(Long tenantId, String studentName, String amount) {
        List<RecipientTarget> targets = schoolStaffDirectory.findSchoolAdminContacts(tenantId).stream()
                .map(gn.lacaeducation.iam.SchoolStaffDirectory.SchoolAdminContact::phone)
                .filter(phone -> phone != null && !phone.isBlank())
                .map(phone -> new RecipientTarget(null, phone))
                .toList();
        if (targets.isEmpty()) {
            log.debug("Aucun compte SCHOOL_ADMIN joignable par SMS pour le tenant {}; alerte fondateur ignorée", tenantId);
            return;
        }
        String school = schoolName(tenantId);
        String content = "[" + school + "] Paiement de " + amount + " reçu pour " + studentName + ".";
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, CommunicationChannel.SMS,
                RecipientType.GROUP, "Paiement reçu", content, CommunicationReason.GENERAL, targets));
    }

    private String destinationFor(ParentPublicInfo parent, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? parent.email() : parent.phone();
    }

    private String studentDestination(StudentPublicInfo student, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? student.email() : student.phone();
    }

    private String schoolName(Long tenantId) {
        if (tenantId == null) return "L'établissement";
        return tenantInfoProvider.findTenantInfo(tenantId).map(TenantPublicInfo::schoolName).orElse("L'établissement");
    }

    private String fullName(StudentPublicInfo s) {
        return ((s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "")).trim();
    }
}
