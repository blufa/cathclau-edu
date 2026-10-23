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
    @Override public Optional<Parent> findByUserId(Long userId) { return parentRepo.findByUserId(userId).map(mapper::toDomain); }
    @Override public List<Parent> findAllByUserId(Long userId) { return parentRepo.findAllByUserId(userId).stream().map(mapper::toDomain).toList(); }
    @Override public List<Parent> findAllByLinkedStaff(gn.lacaeducation.students.domain.StaffLinkType staffType, Long staffId) {
        return parentRepo.findAllByLinkedStaffTypeAndLinkedStaffId(staffType, staffId).stream().map(mapper::toDomain).toList();
    }
    @Override public Optional<Parent> findByIdempotencyKey(String key) { return parentRepo.findByIdempotencyKey(key).map(mapper::toDomain); }
    @Override public Optional<Parent> findByTenantIdAndPhone(Long tenantId, String phone) {
        return parentRepo.findByTenantIdAndPhone(tenantId, phone).map(mapper::toDomain);
    }
    @Override public List<Parent> findByTenantId(Long t) { return parentRepo.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { parentRepo.deleteById(id); }
    @Override public boolean existsById(Long id) { return parentRepo.existsById(id); }
    @Override public List<String> suggestFirstNames(Long t, String q, int limit) {
        return parentRepo.suggestFirstNames(t, q, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    @Override public List<String> suggestLastNames(Long t, String q, int limit) {
        return parentRepo.suggestLastNames(t, q, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    @Override public List<String> suggestAddresses(Long t, String q, int limit) {
        return parentRepo.suggestAddresses(t, q, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override public StudentParent save(StudentParent sp) { return mapper.toDomain(studentParentRepo.save(mapper.toEntity(sp))); }
    @Override public List<Parent> findParentsByStudentId(Long sid) {
        java.util.Map<Long, Boolean> primary = studentParentRepo.findByStudentId(sid).stream()
                .collect(java.util.stream.Collectors.toMap(EleveParentJpaEntity::getParentId,
                        EleveParentJpaEntity::isPrimaryGuardian, (a, b) -> a || b));
        return studentParentRepo.findParentsByStudentId(sid).stream().map(e -> {
            Parent p = mapper.toDomain(e);
            p.setPrimaryGuardian(Boolean.TRUE.equals(primary.get(p.getId())));
            return p;
        }).toList();
    }
    @Override public List<StudentParent> findLinksByStudentId(Long sid) { return studentParentRepo.findByStudentId(sid).stream().map(mapper::toDomain).toList(); }
    @Override public void unlinkStudentParent(Long id) { studentParentRepo.deleteById(id); }
    @Override public void unlinkByStudentAndParent(Long studentId, Long parentId) { studentParentRepo.deleteByStudentIdAndParentId(studentId, parentId); }
    @Override public void unlinkAllByParentId(Long parentId) { studentParentRepo.deleteByParentId(parentId); }
    @Override public List<Long> findStudentIdsByParentId(Long parentId) {
        return studentParentRepo.findByParentId(parentId).stream()
                .map(gn.lacaeducation.students.adapters.out.persistence.EleveParentJpaEntity::getStudentId).toList();
    }
    @Override public List<Long> findDistinctLinkedParentIds() { return studentParentRepo.findDistinctParentIds(); }
}
