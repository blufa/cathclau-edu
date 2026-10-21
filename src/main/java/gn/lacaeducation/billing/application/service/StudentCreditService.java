package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.StudentCreditUseCase;
import gn.lacaeducation.billing.application.port.out.StudentCreditRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.StudentCredit;
import gn.lacaeducation.billing.domain.StudentCreditUsage;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentCreditService implements StudentCreditUseCase {

    private final StudentCreditRepository repo;
    private final InvoiceUseCase invoiceUseCase;

    @Override
    public StudentCredit create(Long tenantId, Long studentId, BigDecimal amount, Long sourcePaymentId,
                                 Long sourceInvoiceId, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Le montant du crédit doit être positif");
        }
        return repo.save(StudentCredit.builder()
                .tenantId(tenantId).studentId(studentId).amount(amount).remainingAmount(amount)
                .sourcePaymentId(sourcePaymentId).sourceInvoiceId(sourceInvoiceId)
                .reason(reason).createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal availableBalance(Long studentId) {
        return repo.findByStudentId(studentId).stream()
                .map(StudentCredit::getRemainingAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCredit> findByStudentId(Long studentId) {
        return repo.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCreditUsage> findUsageHistory(Long studentId) {
        return repo.findUsagesByStudentId(studentId);
    }

    @Override
    public BigDecimal applyToInvoice(Long studentId, Long invoiceId, BigDecimal amount, Long appliedByUserId) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Le montant à appliquer doit être positif");
        }
        Invoice invoice = invoiceUseCase.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        // Garde-fou explicite (§4 : "ne jamais utiliser le crédit d'un élève pour un autre élève") —
        // même si l'appelant (contrôleur) est censé déjà filtrer par élève, on ne fait jamais confiance
        // au seul studentId du chemin d'URL pour une opération financière.
        if (!invoice.getStudentId().equals(studentId)) {
            throw new BusinessRuleException("Cette facture n'appartient pas à cet élève — crédit non appliqué");
        }

        BigDecimal remainingToApply = amount.min(invoice.getRemainingBalance());
        if (remainingToApply.signum() <= 0) {
            throw new BusinessRuleException("Cette facture est déjà soldée");
        }
        // Vérification préalable (hors verrou) : refuse tout de suite une demande qu'on sait déjà
        // insuffisante, plutôt que de verrouiller des lots pour rien. Le contrôle décisif reste la
        // comparaison finale ci-dessous (sous verrou, donc fiable face à la concurrence) — celui-ci
        // n'est qu'un repli rapide pour le cas (très majoritaire) sans contention.
        if (availableBalance(studentId).compareTo(remainingToApply) < 0) {
            throw new BusinessRuleException("Crédit disponible insuffisant pour cet élève");
        }
        BigDecimal applied = BigDecimal.ZERO;
        BigDecimal requested = remainingToApply;

        // FIFO : les lots les plus anciens d'abord (findByStudentId trie déjà par createdAt croissant).
        for (StudentCredit lot : repo.findByStudentId(studentId)) {
            if (remainingToApply.signum() <= 0) break;
            if (lot.isExhausted()) continue;

            // Verrou pessimiste pris lot par lot, relu sous verrou avant décrément — un encaissement
            // concurrent qui tenterait de consommer le même lot attend ce verrou plutôt que de lire
            // un remainingAmount périmé (§4 : "appels simultanés ne consomment pas deux fois le crédit").
            StudentCredit locked = repo.lockById(lot.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("StudentCredit", lot.getId()));
            if (locked.isExhausted()) continue;

            BigDecimal fromThisLot = remainingToApply.min(locked.getRemainingAmount());
            locked.setRemainingAmount(locked.getRemainingAmount().subtract(fromThisLot));
            repo.save(locked);

            invoiceUseCase.applyCredit(invoiceId, fromThisLot, "Crédit disponible (trop-perçu)");
            repo.saveUsage(StudentCreditUsage.builder()
                    .tenantId(invoice.getTenantId()).studentId(studentId).creditId(locked.getId())
                    .targetInvoiceId(invoiceId).amount(fromThisLot).appliedByUserId(appliedByUserId)
                    .appliedAt(LocalDateTime.now())
                    .build());

            remainingToApply = remainingToApply.subtract(fromThisLot);
            applied = applied.add(fromThisLot);
        }

        // Filet de sécurité sous verrou : si la disponibilité a changé entre la vérification préalable
        // et l'application (course concurrente malgré tout), on n'applique JAMAIS un montant partiel
        // silencieux — l'exception annule toute la transaction (les applyCredit/saveUsage déjà faits
        // dans cette boucle sont réellement annulés, @Transactional par défaut sur RuntimeException).
        if (applied.compareTo(requested) < 0) {
            throw new BusinessRuleException("Crédit disponible insuffisant pour cet élève (concurrence détectée, réessayez)");
        }
        return applied;
    }
}
