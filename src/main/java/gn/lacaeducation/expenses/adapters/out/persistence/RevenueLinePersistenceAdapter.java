package gn.lacaeducation.expenses.adapters.out.persistence;

import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.RevenueLineRepository;
import gn.lacaeducation.expenses.domain.RevenueLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class RevenueLinePersistenceAdapter implements RevenueLineRepository {
    private final RevenueLineJpaRepository jpa;
    private final ExpensesMapper mapper;

    @Override public RevenueLine save(RevenueLine r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<RevenueLine> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<RevenueLine> findBySheetId(Long sheetId) { return jpa.findBySheetId(sheetId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
