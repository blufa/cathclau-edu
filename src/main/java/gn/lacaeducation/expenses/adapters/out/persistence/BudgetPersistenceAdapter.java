package gn.lacaeducation.expenses.adapters.out.persistence;

import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.BudgetRepository;
import gn.lacaeducation.expenses.domain.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BudgetPersistenceAdapter implements BudgetRepository {
    private final BudgetJpaRepository jpa;
    private final ExpensesMapper mapper;

    @Override public Budget save(Budget b) { return mapper.toDomain(jpa.save(mapper.toEntity(b))); }
    @Override public Optional<Budget> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Budget> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
