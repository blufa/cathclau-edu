package gn.lacaeducation.notification.application.service;
import gn.lacaeducation.notification.adapters.out.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service("dispatchNotificationService") @RequiredArgsConstructor @Slf4j
public class NotificationService {
    private final EmailService emailService;

    public void sendEnrollmentConfirmation(String to, String schoolName, String studentName, String className, String year) {
        emailService.send(to, "Confirmation d'inscription – " + schoolName,
                "email/enrollment-confirmation",
                Map.of("schoolName", schoolName, "studentName", studentName,
                       "className", className, "academicYear", year));
    }

    public void sendInvoiceNotification(String to, String schoolName, String studentName,
                                         String ref, String amount, String dueDate) {
        emailService.send(to, "Nouvelle facture – " + schoolName,
                "email/invoice-generated",
                Map.of("schoolName", schoolName, "studentName", studentName,
                       "invoiceRef", ref, "amount", amount, "dueDate", dueDate));
    }

    public void sendPaymentReceipt(String to, String schoolName, String studentName,
                                    String receiptRef, String amount, String date, String channel) {
        emailService.send(to, "Reçu de paiement – " + schoolName,
                "email/payment-receipt",
                Map.of("schoolName", schoolName, "studentName", studentName,
                       "receiptRef", receiptRef, "amount", amount, "paymentDate", date, "channel", channel));
    }

    public void sendAbsenceNotification(String to, String schoolName, String studentName,
                                         String date, String className) {
        emailService.send(to, "Absence signalée – " + studentName,
                "email/absence-notification",
                Map.of("schoolName", schoolName, "studentName", studentName,
                       "date", date, "className", className));
    }

    public void sendReportCardPublished(String to, String schoolName, String studentName,
                                         String period, String average, String rank, String classSize, String mention) {
        emailService.send(to, "Bulletin disponible – " + period,
                "email/report-card-published",
                Map.of("schoolName", schoolName, "studentName", studentName,
                       "period", period, "average", average, "rank", rank,
                       "classSize", classSize, "mention", mention));
    }

    public void sendWelcome(String to, String schoolName, String firstName, String lastName, String loginUrl) {
        emailService.send(to, "Bienvenue sur " + schoolName,
                "email/welcome",
                Map.of("schoolName", schoolName, "firstName", firstName,
                       "lastName", lastName, "loginUrl", loginUrl, "email", to));
    }
}
