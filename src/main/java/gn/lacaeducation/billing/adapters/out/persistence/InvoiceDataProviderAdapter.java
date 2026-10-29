package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.InvoiceDataProvider;
import gn.lacaeducation.billing.InvoicePublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class InvoiceDataProviderAdapter implements InvoiceDataProvider {
    private final InvoiceJpaRepository jpa;
    private final InstallmentJpaRepository installmentJpa;
    private final SchoolFeeJpaRepository schoolFeeJpa;
    @Override
    public Optional<InvoicePublicInfo> findInvoice(Long invoiceId) {
        return jpa.findById(invoiceId).map(e ->
                new InvoicePublicInfo(e.getId(), e.getReference(), e.getTotalAmount(), e.getRemainingBalance(), e.getIssueDate()));
    }
    @Override
    public Optional<InvoicePublicInfo> findByReference(String reference) {
        return jpa.findByReference(reference).map(e ->
                new InvoicePublicInfo(e.getId(), e.getReference(), e.getTotalAmount(), e.getRemainingBalance(), e.getIssueDate()));
    }
    @Override
    public Optional<String> findInstallmentLabel(Long installmentId) {
        if (installmentId == null) return Optional.empty();
        return installmentJpa.findById(installmentId).map(InstallmentJpaEntity::getLabel);
    }
    private static final java.util.Map<gn.lacaeducation.billing.domain.FeeType, String> FEE_TYPE_LABEL = java.util.Map.of(
            gn.lacaeducation.billing.domain.FeeType.ENROLLMENT, "Inscription",
            gn.lacaeducation.billing.domain.FeeType.RE_ENROLLMENT, "Réinscription",
            gn.lacaeducation.billing.domain.FeeType.MONTHLY, "Mensualité",
            gn.lacaeducation.billing.domain.FeeType.TRANSPORT, "Transport",
            gn.lacaeducation.billing.domain.FeeType.CANTEEN, "Cantine",
            gn.lacaeducation.billing.domain.FeeType.EXAM, "Examen",
            gn.lacaeducation.billing.domain.FeeType.OTHER, "Autre",
            gn.lacaeducation.billing.domain.FeeType.PLATFORM, "Frais plateforme");

    @Override
    public Optional<String> findSchoolFeeLabel(Long feeId) {
        if (feeId == null) return Optional.empty();
        // label n'est renseigné que pour OTHER/PLATFORM (voir SchoolFee#label) — repli sur le libellé
        // FRANÇAIS du type pour les autres (Mensualité, Inscription…), jamais le nom technique brut de
        // l'enum (§retour comptable, même règle que InvoiceService#feeLabel).
        return schoolFeeJpa.findById(feeId).map(e ->
                e.getLabel() != null && !e.getLabel().isBlank() ? e.getLabel() : FEE_TYPE_LABEL.getOrDefault(e.getType(), e.getType().name()));
    }
}
