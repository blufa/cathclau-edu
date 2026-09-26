package gn.lacaeducation.expenses.adapters.out.persistence;

import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.MonthlyExpenseSheetRepository;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class MonthlyExpenseSheetPersistenceAdapter implements MonthlyExpenseSheetRepository {
    private final MonthlyExpenseSheetJpaRepository jpa;
    private final ExpensesMapper mapper;

    @Override public MonthlyExpenseSheet save(MonthlyExpenseSheet s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<MonthlyExpenseSheet> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<MonthlyExpenseSheet> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(mapper::toDomain).toList();
    }
}
