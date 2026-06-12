package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.application.port.out.StudentDocumentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class ElevePersistenceAdapter implements StudentRepository, StudentDocumentRepository {

    private final EleveJpaRepository eleveRepo;
    private final DocumentEleveJpaRepository docRepo;
    private final StudentsMapper mapper;

    @Override public Student save(Student s) { return mapper.toDomain(eleveRepo.save(mapper.toEntity(s))); }
    @Override public Optional<Student> findById(Long id) { return eleveRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Student> findByTenantId(Long t) { return eleveRepo.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public org.springframework.data.domain.Page<Student> search(Long t, gn.lacaeducation.students.domain.Gender gender, String q, Long classId, gn.lacaeducation.students.domain.EnrollmentStatus status, org.springframework.data.domain.Pageable pageable) {
        return eleveRepo.search(t, gender, q, classId, status, pageable).map(mapper::toDomain);
    }
    @Override public void deleteById(Long id) { eleveRepo.deleteById(id); }
    @Override public boolean existsById(Long id) { return eleveRepo.existsById(id); }
    @Override public long countByTenantId(Long t) { return eleveRepo.countByTenantId(t); }
    @Override public boolean existsByTenantIdAndStudentNumber(Long t, String num) { return eleveRepo.existsByTenantIdAndStudentNumber(t, num); }

    @Override public StudentDocument save(StudentDocument d) { return mapper.toDomain(docRepo.save(mapper.toEntity(d))); }
    @Override public List<StudentDocument> findByStudentId(Long sid) { return docRepo.findByStudentId(sid).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteDocumentById(Long id) { docRepo.deleteById(id); }
}
