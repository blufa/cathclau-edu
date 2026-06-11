package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.application.port.in.ParentUseCase;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Parent;
import gn.lacaeducation.students.domain.Relationship;
import gn.lacaeducation.students.domain.Student;
import gn.lacaeducation.students.domain.StudentParent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service @Transactional @RequiredArgsConstructor
public class ParentService implements ParentUseCase {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentRepository studentRepository;

    @Override
    public Parent create(CreateParentCommand cmd) {
        return parentRepository.save(Parent.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId()).firstName(cmd.firstName())
                .lastName(cmd.lastName()).phone(cmd.phone()).email(cmd.email())
                .occupation(cmd.occupation()).address(cmd.address()).build());
    }

    @Override
    public Parent update(Long id, UpdateParentCommand cmd) {
        Parent p = parentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + id));
        p.setFirstName(cmd.firstName()); p.setLastName(cmd.lastName()); p.setPhone(cmd.phone());
        p.setEmail(cmd.email()); p.setOccupation(cmd.occupation()); p.setAddress(cmd.address());
        return parentRepository.save(p);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Parent> findById(Long id) { return parentRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public List<Parent> findByTenantId(Long tenantId) { return parentRepository.findByTenantId(tenantId); }

    @Override
    public void delete(Long id) { parentRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public boolean existsById(Long id) { return parentRepository.existsById(id); }

    @Override
    public StudentParent linkStudent(Long studentId, Long parentId, Relationship relationship) {
        return studentParentRepository.save(StudentParent.builder()
                .studentId(studentId).parentId(parentId).relationship(relationship).build());
    }

    @Override
    public void unlinkStudent(Long studentParentId) { studentParentRepository.unlinkStudentParent(studentParentId); }

    @Override @Transactional(readOnly = true)
    public List<Parent> findByStudent(Long studentId) { return studentParentRepository.findParentsByStudentId(studentId); }

    @Override @Transactional(readOnly = true)
    public List<Student> findStudentsByParent(Long parentId) {
        return studentParentRepository.findStudentIdsByParentId(parentId).stream()
                .flatMap(id -> studentRepository.findById(id).stream()).toList();
    }
}
