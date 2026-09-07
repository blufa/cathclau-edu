package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fournit les informations de l'élève payeur, de l'agent encaisseur et du type de frais réglé au
 * module {@code payments} (pour l'impression du reçu). Vit dans {@code billing}, qui connaît déjà
 * élèves, factures et paiements : implémente le port {@link PayerDirectory} possédé par payments,
 * évitant une dépendance directe payments → students/iam (cycle : platform dépend de payments, iam
 * de platform).
 */
@Component
@RequiredArgsConstructor
public class BillingPayerDirectoryAdapter implements PayerDirectory {

    private final StudentDataProvider studentDataProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final UserPublicInfoProvider userPublicInfoProvider;
    private final InvoiceRepository invoiceRepository;
    private final SchoolFeeRepository schoolFeeRepository;

    private static final java.util.Map<FeeType, String> FEE_TYPE_LABEL = java.util.Map.of(
            FeeType.ENROLLMENT, "Inscription",
            FeeType.RE_ENROLLMENT, "Réinscription",
            FeeType.MONTHLY, "Mensualité",
            FeeType.TRANSPORT, "Transport",
            FeeType.CANTEEN, "Cantine",
            FeeType.EXAM, "Examen");

    @Override
    public Optional<String> studentName(Long studentId) {
        if (studentId == null) return Optional.empty();
        return studentDataProvider.findStudent(studentId)
                .map(s -> ((s.firstName() == null ? "" : s.firstName()) + " "
                        + (s.lastName() == null ? "" : s.lastName())).trim())
                .filter(n -> !n.isBlank());
    }

    @Override
    public Optional<String> studentNumber(Long studentId) {
        if (studentId == null) return Optional.empty();
        return studentDataProvider.findStudent(studentId).map(s -> s.studentNumber());
    }

    @Override
    public Optional<String> className(Long studentId) {
        if (studentId == null) return Optional.empty();
        return studentDataProvider.findCurrentClassId(studentId)
                .flatMap(academicsDataProvider::findClass)
                .map(c -> c.label());
    }

    @Override
    public Optional<String> operatorName(Long userId) {
        if (userId == null) return Optional.empty();
        return userPublicInfoProvider.findUser(userId)
                .map(u -> ((u.firstName() == null ? "" : u.firstName()) + " "
                        + (u.lastName() == null ? "" : u.lastName())).trim())
                .filter(n -> !n.isBlank());
    }

    @Override
    public Optional<String> currentAcademicYearLabel(Long tenantId) {
        if (tenantId == null) return Optional.empty();
        return academicsDataProvider.currentAcademicYearLabel(tenantId);
    }

    @Override
    public Optional<InvoiceFeeInfo> invoiceFeeInfo(Long invoiceId) {
        if (invoiceId == null) return Optional.empty();
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null || invoice.getFeeId() == null) return Optional.empty();
        FeeType type = schoolFeeRepository.findById(invoice.getFeeId()).map(f -> f.getType()).orElse(null);
        if (type == null) return Optional.empty();
        return Optional.of(new InvoiceFeeInfo(FEE_TYPE_LABEL.getOrDefault(type, type.name()), invoice.getPeriodLabel(),
                invoice.getDiscountAmount(), invoice.getDiscountReason(), invoice.getPlatformFeeAmount()));
    }
}
