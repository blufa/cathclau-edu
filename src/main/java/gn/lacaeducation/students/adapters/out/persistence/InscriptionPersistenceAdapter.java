package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.domain.Enrollment;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class InscriptionPersistenceAdapter implements EnrollmentRepository {

    private final InscriptionJpaRepository repo;
    private final StudentsMapper mapper;

    @Override public Enrollment save(Enrollment e) { return mapper.toDomain(repo.save(mapper.toEntity(e))); }
    @Override public Optional<Enrollment> findById(Long id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public List<Enrollment> findByStudentId(Long sid) { return repo.findByStudentId(sid).stream().map(mapper::toDomain).toList(); }
    @Override public List<Enrollment> findByClassId(Long cid) { return repo.findByClassId(cid).stream().map(mapper::toDomain).toList(); }
    @Override public List<Enrollment> findByAcademicYearId(Long aid) { return repo.findByAcademicYearId(aid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { repo.deleteById(id); }
    @Override public boolean existsById(Long id) { return repo.existsById(id); }
    @Override public long countByClassIdAndStatus(Long cid, EnrollmentStatus status) { return repo.countByClassIdAndStatus(cid, status); }
    @Override public boolean existsValidatedForStudentYear(Long sid, Long aid) {
        return repo.existsByStudentIdAndAcademicYearIdAndStatus(sid, aid, EnrollmentStatus.VALIDATED);
    }
    @Override public List<Enrollment> search(Long sid, Long cid, Long aid, EnrollmentStatus status) {
        return repo.search(sid, cid, aid, status).stream().map(mapper::toDomain).toList();
    }
}
