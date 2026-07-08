package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.AnnualBudgetRepository;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class AnnualBudgetPersistenceAdapter implements AnnualBudgetRepository {
    private final AnnualBudgetJpaRepository jpa;
    private final ExpensesMapper mapper;
    @Override public AnnualBudget save(AnnualBudget b) { return mapper.toDomain(jpa.save(mapper.toEntity(b))); }
    @Override public Optional<AnnualBudget> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<AnnualBudget> findByTenantId(Long t) { return jpa.findByTenantIdOrderByAcademicYearDesc(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
