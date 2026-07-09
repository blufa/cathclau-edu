package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.adapters.out.persistence.mapper.PayrollMapper;
import gn.lacaeducation.payroll.application.port.out.SalaryStructureRepository;
import gn.lacaeducation.payroll.domain.SalaryStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SalaryStructurePersistenceAdapter implements SalaryStructureRepository {
    private final SalaryStructureJpaRepository jpa;
    private final PayrollMapper mapper;

    @Override public SalaryStructure save(SalaryStructure s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<SalaryStructure> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<SalaryStructure> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
