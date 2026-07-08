package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.ExemptionUseCase;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
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
                .percentage(percentage).amount(amount).feeType(c.feeType()).reason(c.reason())
                .startDate(c.startDate()).endDate(c.endDate()).validatedBy(validatedBy)
                .active(true).createdAt(LocalDateTime.now())
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
}
