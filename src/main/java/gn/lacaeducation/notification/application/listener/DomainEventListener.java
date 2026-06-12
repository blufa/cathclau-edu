package gn.lacaeducation.notification.application.listener;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.InvoiceDataProvider;
import gn.lacaeducation.billing.InvoicePublicInfo;
import gn.lacaeducation.platform.TenantInfoProvider;
import gn.lacaeducation.platform.TenantPublicInfo;
import gn.lacaeducation.notification.application.service.NotificationService;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;
import java.util.List;
import gn.lacaeducation.students.ParentPublicInfo;

@Component @RequiredArgsConstructor @Slf4j
public class DomainEventListener {

    private final NotificationService notificationService;
    private final StudentDataProvider studentDataProvider;
    private final InvoiceDataProvider invoiceDataProvider;
    private final PaymentDataProvider paymentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final TenantInfoProvider tenantInfoProvider;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @org.springframework.transaction.event.TransactionalEventListener
    public void onEnrollmentValidated(EnrollmentValidatedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String schoolName = schoolName(event.tenantId());
            String className = academicsDataProvider.findClass(event.classId())
                    .map(c -> c.label()).orElse("N/A");
            // notify student
            if (hasEmail(student)) {
                notificationService.sendEnrollmentConfirmation(student.email(), schoolName,
                        fullName(student), className, String.valueOf(event.academicYearId()));
            }
            // notify parents
            sendToParents(event.studentId(), email ->
                    notificationService.sendEnrollmentConfirmation(email, schoolName,
                            fullName(student), className, String.valueOf(event.academicYearId())));
        });
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void onInvoiceGenerated(InvoiceGeneratedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            invoiceDataProvider.findInvoice(event.invoiceId()).ifPresent(invoice -> {
                String schoolName = schoolName(event.tenantId());
                String amount = invoice.totalAmount() + " FCFA";
                String dueDate = invoice.issueDate() != null ? invoice.issueDate().format(DATE_FMT) : "N/A";
                sendToParents(event.studentId(), email ->
                        notificationService.sendInvoiceNotification(email, schoolName,
                                fullName(student), invoice.reference(), amount, dueDate));
                if (hasEmail(student)) {
                    notificationService.sendInvoiceNotification(student.email(), schoolName,
                            fullName(student), invoice.reference(), amount, dueDate);
                }
            });
        });
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            paymentDataProvider.findPayment(event.paymentId()).ifPresent(payment -> {
                String schoolName = schoolName(event.tenantId());
                String date = payment.paidAt() != null ? payment.paidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                String receiptRef = "REC-" + payment.id();
                String amount = payment.amount() + " FCFA";
                sendToParents(event.studentId(), email ->
                        notificationService.sendPaymentReceipt(email, schoolName,
                                fullName(student), receiptRef, amount, date, payment.channel()));
                if (hasEmail(student)) {
                    notificationService.sendPaymentReceipt(student.email(), schoolName,
                            fullName(student), receiptRef, amount, date, payment.channel());
                }
            });
        });
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void onStudentAbsent(StudentAbsentEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String schoolName = schoolName(event.tenantId());
            String className = academicsDataProvider.findClass(event.classId())
                    .map(c -> c.label()).orElse("N/A");
            String date = event.date() != null ? event.date().format(DATE_FMT) : "N/A";
            sendToParents(event.studentId(), email ->
                    notificationService.sendAbsenceNotification(email, schoolName,
                            fullName(student), date, className));
        });
    }

    @org.springframework.transaction.event.TransactionalEventListener
    public void onReportCardPublished(ReportCardPublishedEvent event) {
        studentDataProvider.findStudent(event.studentId()).ifPresent(student -> {
            String schoolName = schoolName(null);
            String period = academicsDataProvider.findPeriod(event.periodId())
                    .map(p -> p.label()).orElse("N/A");
            sendToParents(event.studentId(), email ->
                    notificationService.sendReportCardPublished(email, schoolName,
                            fullName(student), period, "—", "—", "—", "—"));
            if (hasEmail(student)) {
                notificationService.sendReportCardPublished(student.email(), schoolName,
                        fullName(student), period, "—", "—", "—", "—");
            }
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private void sendToParents(Long studentId, java.util.function.Consumer<String> action) {
        List<ParentPublicInfo> parents = studentDataProvider.findParentsByStudent(studentId);
        parents.stream().filter(p -> p.email() != null && !p.email().isBlank()).forEach(p -> action.accept(p.email()));
    }

    private String schoolName(Long tenantId) {
        if (tenantId == null) return "L'établissement";
        return tenantInfoProvider.findTenantInfo(tenantId).map(TenantPublicInfo::schoolName).orElse("L'établissement");
    }

    private String fullName(StudentPublicInfo s) {
        return (s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "");
    }

    private boolean hasEmail(StudentPublicInfo s) {
        return s.email() != null && !s.email().isBlank();
    }
}
