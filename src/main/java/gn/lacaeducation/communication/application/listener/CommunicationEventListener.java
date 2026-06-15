package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.*;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.platform.TenantInfoProvider;
import gn.lacaeducation.platform.TenantPublicInfo;
import gn.lacaeducation.students.ParentPublicInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        studentDataProvider.findStudent(event.studentId()).ifPresent(student ->
            paymentDataProvider.findPayment(event.paymentId()).ifPresent(payment -> {
                String amount = payment.amount() + " FCFA";
                Map<String, String> vars = Map.of(
                        "nom_eleve", fullName(student), "ecole", schoolName(event.tenantId()),
                        "montant", amount, "canal", payment.channel() == null ? "" : payment.channel(),
                        "reference", payment.reference() == null ? "" : payment.reference());
                String defaultBody = "Paiement de " + amount + " reçu pour " + fullName(student) + ". Merci.";
                dispatch(event.tenantId(), CommunicationReason.GENERAL, "Paiement reçu", defaultBody, vars, event.studentId());
            }));
    }

    // ── dispatch ───────────────────────────────────────────────────────────────
    private void dispatch(Long tenantId, CommunicationReason reason, String defaultSubject,
                          String defaultBody, Map<String, String> vars, Long studentId) {
        Optional<MessageTemplate> template = templateRepository.findByTenantIdAndReason(tenantId, reason);
        CommunicationChannel channel = template.map(MessageTemplate::getChannel).orElse(CommunicationChannel.SMS);
        if (channel == null) channel = CommunicationChannel.SMS;
        String subject = template.map(t -> MessageTemplate.render(t.getSubject(), vars)).orElse(defaultSubject);
        String content = template.map(t -> MessageTemplate.render(t.getBody(), vars)).orElse(defaultBody);

        final CommunicationChannel ch = channel;
        List<RecipientTarget> targets = studentDataProvider.findParentsByStudent(studentId).stream()
                .map(parent -> destinationFor(parent, ch))
                .filter(dest -> dest != null && !dest.isBlank())
                .map(dest -> new RecipientTarget(null, dest))
                .toList();
        if (targets.isEmpty()) {
            log.debug("No reachable parent for student {} on channel {}; skipping {} notification", studentId, ch, reason);
            return;
        }
        communicationUseCase.send(new SendCommunicationCommand(tenantId, null, ch,
                RecipientType.PARENT, subject, content, reason, targets));
    }

    private String destinationFor(ParentPublicInfo parent, CommunicationChannel channel) {
        return channel == CommunicationChannel.EMAIL ? parent.email() : parent.phone();
    }

    private String schoolName(Long tenantId) {
        if (tenantId == null) return "L'établissement";
        return tenantInfoProvider.findTenantInfo(tenantId).map(TenantPublicInfo::schoolName).orElse("L'établissement");
    }

    private String fullName(StudentPublicInfo s) {
        return ((s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "")).trim();
    }
}
