package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.port.in.SchoolFeeUseCase;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.SchoolFee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class SchoolFeeService implements SchoolFeeUseCase {
    private final SchoolFeeRepository repo;
    @Override public SchoolFee create(CreateFeeCommand c) {
        return repo.save(SchoolFee.builder().tenantId(c.tenantId()).classId(c.classId())
                .type(c.type()).amount(c.amount()).academicYear(c.academicYear()).build());
    }
    @Override public SchoolFee update(Long id, UpdateFeeCommand c) {
        SchoolFee f = load(id); f.setClassId(c.classId()); f.setType(c.type()); f.setAmount(c.amount()); f.setAcademicYear(c.academicYear());
        return repo.save(f);
    }
    @Override @Transactional(readOnly=true) public Optional<SchoolFee> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<SchoolFee> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<SchoolFee> findByClassId(Long c) { return repo.findByClassId(c); }
    @Override @Transactional(readOnly=true) public List<SchoolFee> findByTenantIdAndAcademicYear(Long t, String y) { return repo.findByTenantIdAndAcademicYear(t, y); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    @Override @Transactional(readOnly=true) public boolean existsById(Long id) { return repo.existsById(id); }
    private SchoolFee load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("SchoolFee not found: " + id)); }
}
