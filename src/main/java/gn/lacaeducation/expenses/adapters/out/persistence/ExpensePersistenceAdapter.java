package gn.lacaeducation.expenses.adapters.out.persistence;

import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.ExpenseRepository;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExpensePersistenceAdapter implements ExpenseRepository {
    private final ExpenseJpaRepository jpa;
    private final ExpensesMapper mapper;

    @Override public Expense save(Expense e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<Expense> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Expense> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Expense> findByTenantIdAndStatus(Long t, ExpenseStatus s) { return jpa.findByTenantIdAndStatus(t, s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
