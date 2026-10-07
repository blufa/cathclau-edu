package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
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
            FeeType.EXAM, "Examen",
            FeeType.OTHER, "Autre");

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
    public Optional<String> operatorPhone(Long userId) {
        if (userId == null) return Optional.empty();
        return userPublicInfoProvider.findUser(userId)
                .map(u -> u.phone())
                .filter(p -> p != null && !p.isBlank());
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
        String feeLabel = feeLabel(invoice);
        if (feeLabel == null) return Optional.empty();
        return Optional.of(new InvoiceFeeInfo(feeLabel, invoice.getPeriodLabel(),
                invoice.getDiscountAmount(), invoice.getDiscountReason(), invoice.getPlatformFeeAmount()));
    }

    @Override
    public List<UnpaidInvoiceInfo> unpaidInvoices(Long studentId) {
        if (studentId == null) return List.of();
        return currentYearInvoices(studentId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.PARTIAL)
                .sorted(Comparator.comparing(Invoice::getIssueDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(i -> new UnpaidInvoiceInfo(fullLabel(i), i.getRemainingBalance()))
                .toList();
    }

    @Override
    public List<InvoiceRowInfo> studentInvoices(Long studentId) {
        if (studentId == null) return List.of();
        return currentYearInvoices(studentId).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .sorted(Comparator.comparing(Invoice::getIssueDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(i -> new InvoiceRowInfo(i.getReference(), i.getTotalAmount(), fullLabel(i),
                        i.getStatus() == InvoiceStatus.PAID, i.getPaidAmount()))
                .toList();
    }

    /** Factures de l'élève limitées à l'année scolaire courante du tenant (§retour comptable) — sans
     * ce filtre, un solde "reste à payer" d'un reçu de paiement sommait aussi des factures d'années
     * révolues, produisant un total incohérent aux yeux du comptable. Si l'année courante ne peut pas
     * être résolue (élève/tenant introuvable), ou si une facture n'a pas d'année renseignée (ancien
     * enregistrement), elle est incluse plutôt que silencieusement perdue. */
    private List<Invoice> currentYearInvoices(Long studentId) {
        List<Invoice> all = invoiceRepository.findByStudentId(studentId);
        Long tenantId = studentDataProvider.findStudent(studentId).map(s -> s.tenantId()).orElse(null);
        Long currentYearId = tenantId == null ? null
                : academicsDataProvider.currentAcademicYear(tenantId).map(y -> y.id()).orElse(null);
        if (currentYearId == null) return all;
        return all.stream()
                .filter(i -> i.getAcademicYearId() == null || currentYearId.equals(i.getAcademicYearId()))
                .toList();
    }

    /** "Mensualité — Novembre 2026" (période) ou simplement "Mensualité" (facture d'un seul mois/sans
     * période, ex. inscription) — même logique que {@link #invoiceFeeInfo}, réutilisée pour la liste
     * des factures encore dues (§communication, SMS d'encaissement). */
    private String fullLabel(Invoice invoice) {
        String label = feeLabel(invoice);
        if (label == null) label = "Facture";
        return invoice.getPeriodLabel() != null && !invoice.getPeriodLabel().isBlank()
                ? label + " — " + invoice.getPeriodLabel() : label;
    }

    /** Nom réel du frais annexe (OTHER, ex. "Tenue scolaire") si configuré, sinon le libellé générique
     * du type — jamais le seul mot "Autre" pour un frais annexe nommé. Null si le frais est introuvable. */
    private String feeLabel(Invoice invoice) {
        if (invoice.getFeeId() == null) return null;
        var fee = schoolFeeRepository.findById(invoice.getFeeId()).orElse(null);
        if (fee == null || fee.getType() == null) return null;
        if (fee.getType() == FeeType.OTHER && fee.getLabel() != null && !fee.getLabel().isBlank()) {
            return fee.getLabel();
        }
        return FEE_TYPE_LABEL.getOrDefault(fee.getType(), fee.getType().name());
    }
}
