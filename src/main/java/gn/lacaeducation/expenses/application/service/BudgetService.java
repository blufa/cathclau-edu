package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.port.out.BudgetRepository;
import gn.lacaeducation.expenses.domain.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** CRUD for annual budget allocations per category. Consumed amounts are aggregated by the front. */
@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repo;

    public record BudgetCommand(Long tenantId, Long categoryId, String academicYear, BigDecimal allocatedAmount) {}

    public Budget save(BudgetCommand c) {
        return repo.save(Budget.builder().tenantId(c.tenantId()).categoryId(c.categoryId())
                .academicYear(c.academicYear()).allocatedAmount(c.allocatedAmount()).build());
    }

    public Budget update(Long id, BigDecimal allocatedAmount) {
        Budget b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget introuvable : " + id));
        b.setAllocatedAmount(allocatedAmount);
        return repo.save(b);
    }

    @Transactional(readOnly = true)
    public List<Budget> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    public void delete(Long id) { repo.deleteById(id); }
}
