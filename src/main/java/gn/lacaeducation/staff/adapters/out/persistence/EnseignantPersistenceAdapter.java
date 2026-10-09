package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class EnseignantPersistenceAdapter implements TeacherRepository {

    private final EnseignantJpaRepository repo;
    private final StaffMapper mapper;

    @Override public Teacher save(Teacher t) { return mapper.toDomain(repo.save(mapper.toEntity(t))); }
    @Override public Optional<Teacher> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<Teacher> findByUserId(Long userId) { return repo.findByUserId(userId).map(mapper::toDomain); }
    @Override public Optional<Teacher> findByIdempotencyKey(String key) { return repo.findByIdempotencyKey(key).map(mapper::toDomain); }
    @Override public List<Teacher> findByTenantId(Long t) { return repo.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
    @Override public boolean existsById(Long id) { return repo.existsById(id); }
    @Override public boolean existsByTenantIdAndStaffNumber(Long t, String num) { return repo.existsByTenantIdAndStaffNumber(t, num); }
    @Override public List<Teacher> findMissingStaffNumber() { return repo.findMissingStaffNumber().stream().map(mapper::toDomain).toList(); }
}
