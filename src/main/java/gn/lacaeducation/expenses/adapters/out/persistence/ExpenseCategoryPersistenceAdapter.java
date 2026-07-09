package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.ExpenseCategoryRepository;
import gn.lacaeducation.expenses.domain.ExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ExpenseCategoryPersistenceAdapter implements ExpenseCategoryRepository {
    private final ExpenseCategoryJpaRepository jpa;
    private final ExpensesMapper mapper;
    @Override public ExpenseCategory save(ExpenseCategory c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<ExpenseCategory> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<ExpenseCategory> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
