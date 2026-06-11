package gn.lacaeducation.academics.adapters.out.persistence;
import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.ClassStudent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ClassePersistenceAdapter implements ClasseRepository {
    private final ClasseJpaRepository classeRepo;
    private final ClasseMatiereJpaRepository cmRepo;
    private final AcademicsEnrollmentJpaRepository enrollmentRepo;
    private final AcademicsStudentJpaRepository studentRepo;
    private final AcademicsMapper mapper;
    @Override public Classe save(Classe c) { return mapper.toDomain(classeRepo.save(mapper.toEntity(c))); }
    @Override public Optional<Classe> findById(Long id) { return classeRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Classe> findByAcademicYearId(Long yrId) { return classeRepo.findByAcademicYearId(yrId).stream().map(mapper::toDomain).toList(); }
    @Override public List<Classe> findByTenantId(Long tenantId) { return classeRepo.findByTenantId(tenantId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { classeRepo.deleteById(id); }
    @Override public ClasseMatiere saveClassSubject(ClasseMatiere cm) { return mapper.toDomain(cmRepo.save(mapper.toEntity(cm))); }
    @Override public void deleteClassSubjectById(Long id) { cmRepo.deleteById(id); }
    @Override public List<ClasseMatiere> findSubjectsByClassId(Long classId) { return cmRepo.findBySchoolClassId(classId).stream().map(mapper::toDomain).toList(); }
    @Override public List<ClassStudent> findStudentsByClassId(Long classId) {
        List<Long> studentIds = enrollmentRepo.findValidatedStudentIdsByClassId(classId);
        if (studentIds.isEmpty()) return List.of();
        return studentRepo.findByIdIn(studentIds).stream()
                .map(s -> new ClassStudent(s.getId(), s.getFirstName(), s.getLastName(), s.getStudentNumber(), s.getEmail()))
                .toList();
    }
}
