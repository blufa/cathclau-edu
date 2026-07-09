package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.adapters.out.persistence.mapper.AccountingMapper;
import gn.lacaeducation.accounting.application.port.out.FiscalYearRepository;
import gn.lacaeducation.accounting.domain.FiscalYear;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class FiscalYearPersistenceAdapter implements FiscalYearRepository {
    private final FiscalYearJpaRepository jpa;
    private final AccountingMapper mapper;
    @Override public FiscalYear save(FiscalYear f) { return mapper.toDomain(jpa.save(mapper.toEntity(f))); }
    @Override public Optional<FiscalYear> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<FiscalYear> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
}
