package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.BudgetLineRepository;
import gn.lacaeducation.expenses.domain.BudgetLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class BudgetLinePersistenceAdapter implements BudgetLineRepository {
    private final BudgetLineJpaRepository jpa;
    private final ExpensesMapper mapper;
    @Override public BudgetLine save(BudgetLine l) { return mapper.toDomain(jpa.save(mapper.toEntity(l))); }
    @Override public Optional<BudgetLine> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<BudgetLine> findByBudgetId(Long bid) { return jpa.findByBudgetId(bid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public void deleteByBudgetId(Long bid) { jpa.deleteByBudgetId(bid); }
}
