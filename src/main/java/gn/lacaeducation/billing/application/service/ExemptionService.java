package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ExemptionService implements ExemptionUseCase {

    private final ExemptionRepository repo;

    @Override
    public Exemption grant(Long tenantId, GrantExemptionCommand c, Long validatedBy) {
        if (c.studentId() == null) throw new BusinessRuleException("Le bénéficiaire (élève) est requis.");
        if (c.reason() == null || c.reason().isBlank()) throw new BusinessRuleException("Le motif est requis.");
        if (c.type() == null) throw new BusinessRuleException("Le type d'exonération est requis.");

        BigDecimal percentage = c.percentage();
        BigDecimal amount = c.amount();
        switch (c.type()) {
            case TOTAL -> percentage = BigDecimal.valueOf(100);
            case PARTIAL -> {
                if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0
                        || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessRuleException("Une exonération partielle exige un pourcentage dans ]0, 100].");
                }
            }
            case EXCEPTIONAL -> {
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Une remise exceptionnelle exige un montant positif.");
                }
            }
        }
        if (c.startDate() != null && c.endDate() != null && c.endDate().isBefore(c.startDate())) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début.");
        }
        return repo.save(Exemption.builder()
                .tenantId(tenantId).studentId(c.studentId()).type(c.type())
                .percentage(percentage).amount(amount).feeTypes(c.feeTypes()).reason(c.reason())
                .startDate(c.startDate()).endDate(c.endDate()).validatedBy(validatedBy)
                .active(true).createdAt(LocalDateTime.now())
                .staffBenefit(c.staffBenefit() != null && c.staffBenefit())
                .build());
    }

    @Override
    public Exemption revoke(Long id) {
        Exemption e = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exemption", id));
        e.setActive(false);
        return repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exemption> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exemption> findByStudent(Long studentId) {
        return repo.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicableExemption computeApplicable(Long studentId, FeeType feeType) {
        LocalDate today = LocalDate.now();
        List<Exemption> active = repo.findByStudentId(studentId).stream()
                .filter(Exemption::isActive)
                .filter(e -> e.getFeeTypes() == null || e.getFeeTypes().isEmpty() || e.getFeeTypes().contains(feeType))
                .filter(e -> e.getStartDate() == null || !today.isBefore(e.getStartDate()))
                .filter(e -> e.getEndDate() == null || !today.isAfter(e.getEndDate()))
                .toList();

        BigDecimal bestPercentage = BigDecimal.ZERO;
        String percentageReason = null;
        boolean bestIsStaffBenefit = false;
        BigDecimal exceptionalTotal = BigDecimal.ZERO;
        List<String> exceptionalReasons = new java.util.ArrayList<>();
        for (Exemption e : active) {
            if (e.getType() == ExemptionType.EXCEPTIONAL) {
                // Décisions ponctuelles documentées : chacune s'ajoute aux autres (pas de "meilleure"
                // à choisir, ce sont des remises distinctes accordées pour des motifs distincts).
                if (e.getAmount() != null) {
                    exceptionalTotal = exceptionalTotal.add(e.getAmount());
                    exceptionalReasons.add(e.getReason());
                }
            } else if (e.getPercentage() != null && e.getPercentage().compareTo(bestPercentage) > 0) {
                bestPercentage = e.getPercentage();
                percentageReason = e.getReason();
                bestIsStaffBenefit = e.isStaffBenefit();
            }
        }
        return new ApplicableExemption(bestPercentage, percentageReason, bestIsStaffBenefit,
                exceptionalTotal, exceptionalReasons.isEmpty() ? null : String.join(", ", exceptionalReasons));
    }
}
