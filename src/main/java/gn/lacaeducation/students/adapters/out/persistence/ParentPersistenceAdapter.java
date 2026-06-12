package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.StudentParent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class ParentPersistenceAdapter implements ParentRepository, StudentParentRepository {

    private final ParentJpaRepository parentRepo;
    private final EleveParentJpaRepository studentParentRepo;
    private final StudentsMapper mapper;

    @Override public Parent save(Parent p) { return mapper.toDomain(parentRepo.save(mapper.toEntity(p))); }
    @Override public Optional<Parent> findById(Long id) { return parentRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Parent> findByTenantId(Long t) { return parentRepo.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { parentRepo.deleteById(id); }
    @Override public boolean existsById(Long id) { return parentRepo.existsById(id); }

    @Override public StudentParent save(StudentParent sp) { return mapper.toDomain(studentParentRepo.save(mapper.toEntity(sp))); }
    @Override public List<Parent> findParentsByStudentId(Long sid) { return studentParentRepo.findParentsByStudentId(sid).stream().map(mapper::toDomain).toList(); }
    @Override public void unlinkStudentParent(Long id) { studentParentRepo.deleteById(id); }
    @Override public void unlinkByStudentAndParent(Long studentId, Long parentId) { studentParentRepo.deleteByStudentIdAndParentId(studentId, parentId); }
    @Override public List<Long> findStudentIdsByParentId(Long parentId) {
        return studentParentRepo.findByParentId(parentId).stream()
                .map(gn.lacaeducation.students.adapters.out.persistence.EleveParentJpaEntity::getStudentId).toList();
    }
}
