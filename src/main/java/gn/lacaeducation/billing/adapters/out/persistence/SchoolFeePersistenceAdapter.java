package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class SchoolFeePersistenceAdapter implements SchoolFeeRepository {
    private final SchoolFeeJpaRepository jpa;
    private final BillingMapper mapper;
    @Override public SchoolFee save(SchoolFee f) { return mapper.toDomain(jpa.save(mapper.toEntity(f))); }
    @Override public Optional<SchoolFee> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<SchoolFee> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<SchoolFee> findByClassId(Long c) { return jpa.findByClassId(c).stream().map(mapper::toDomain).toList(); }
    @Override public List<SchoolFee> findByClassIdAndType(Long c, FeeType type) { return jpa.findByClassIdAndType(c, type).stream().map(mapper::toDomain).toList(); }
    @Override public List<SchoolFee> findByTenantIdAndAcademicYear(Long t, String y) { return jpa.findByTenantIdAndAcademicYear(t, y).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpa.existsById(id); }
}
