package gn.lacaeducation.accounting.application.service;

import gn.lacaeducation.accounting.application.port.out.FiscalYearRepository;
import gn.lacaeducation.accounting.domain.FiscalYear;
import gn.lacaeducation.accounting.domain.FiscalYearStatus;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Fiscal years: a single OPEN year at a time; closing is one-way. */
@Service
@Transactional
@RequiredArgsConstructor
public class FiscalYearService {

    private final FiscalYearRepository repo;

    public record CreateFiscalYearCommand(Long tenantId, String label, LocalDate startDate, LocalDate endDate, Long academicYearId) {}

    public FiscalYear create(CreateFiscalYearCommand c) {
        return repo.save(FiscalYear.builder().tenantId(c.tenantId()).label(c.label())
                .startDate(c.startDate()).endDate(c.endDate()).academicYearId(c.academicYearId())
                .status(FiscalYearStatus.OPEN).build());
    }

    @Transactional(readOnly = true)
    public List<FiscalYear> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    public FiscalYear close(Long id) {
        FiscalYear f = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exercice introuvable : " + id));
        if (f.getStatus() == FiscalYearStatus.CLOSED) throw new BusinessRuleException("Exercice déjà clôturé");
        f.setStatus(FiscalYearStatus.CLOSED);
        return repo.save(f);
    }
}
