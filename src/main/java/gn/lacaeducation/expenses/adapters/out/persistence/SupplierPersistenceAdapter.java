package gn.lacaeducation.expenses.adapters.out.persistence;

import gn.lacaeducation.expenses.adapters.out.persistence.mapper.ExpensesMapper;
import gn.lacaeducation.expenses.application.port.out.SupplierRepository;
import gn.lacaeducation.expenses.domain.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SupplierPersistenceAdapter implements SupplierRepository {
    private final SupplierJpaRepository jpa;
    private final ExpensesMapper mapper;

    @Override public Supplier save(Supplier s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<Supplier> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Supplier> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
