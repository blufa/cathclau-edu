package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.PersonName;
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
        // Rejoué après une coupure réseau (mode hors-ligne) avec la même clé : renvoie le parent déjà
        // créé au lieu d'en créer un second — même pattern que l'idempotence des élèves/enseignants.
        if (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank()) {
            Optional<Parent> existing = parentRepository.findByIdempotencyKey(cmd.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return parentRepository.save(Parent.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId())
                .firstName(PersonName.capitalizeFirstName(cmd.firstName()))
                .lastName(PersonName.upperCaseLastName(cmd.lastName()))
                .phone(cmd.phone()).email(cmd.email())
                .occupation(cmd.occupation()).address(cmd.address()).idempotencyKey(cmd.idempotencyKey()).build());
    }

    @Override
    public Parent update(Long id, UpdateParentCommand cmd) {
        Parent p = parentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + id));
        p.setFirstName(PersonName.capitalizeFirstName(cmd.firstName()));
        p.setLastName(PersonName.upperCaseLastName(cmd.lastName()));
        p.setPhone(cmd.phone());
        p.setEmail(cmd.email()); p.setOccupation(cmd.occupation()); p.setAddress(cmd.address());
        return parentRepository.save(p);
    }

    @Override
    public Parent updateOwnProfile(Long userId, UpdateProfileCommand cmd) {
        Parent p = requireByUserId(userId);
        // Mise à jour partielle : seuls les champs fournis sont modifiés (le nom n'est pas modifiable).
        if (cmd.phone() != null) p.setPhone(cmd.phone());
        if (cmd.email() != null) p.setEmail(cmd.email());
        if (cmd.address() != null) p.setAddress(cmd.address());
        if (cmd.occupation() != null) p.setOccupation(cmd.occupation());
        return parentRepository.save(p);
    }

    @Override
    public Parent updateOwnPhoto(Long userId, String photoUrl) {
        Parent p = requireByUserId(userId);
        p.setPhoto(photoUrl);
        return parentRepository.save(p);
    }

    private Parent requireByUserId(Long userId) {
        return parentRepository.findByUserId(userId)
                .orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("Parent (compte)", userId));
    }

    @Override @Transactional(readOnly = true)
    public Optional<Parent> findById(Long id) { return parentRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public Optional<Parent> findByUserId(Long userId) { return parentRepository.findByUserId(userId); }

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
    public void setPrimaryGuardian(Long studentId, Long parentId) {
        List<StudentParent> links = studentParentRepository.findLinksByStudentId(studentId);
        if (links.stream().noneMatch(l -> l.getParentId().equals(parentId))) {
            throw new gn.lacaeducation.common.ResourceNotFoundException("StudentParent (student/parent)", parentId);
        }
        for (StudentParent l : links) {
            boolean primary = l.getParentId().equals(parentId);
            if (l.isPrimaryGuardian() != primary) {
                l.setPrimaryGuardian(primary);
                studentParentRepository.save(l);
            }
        }
    }

    @Override
    public void unlinkStudent(Long studentParentId) { studentParentRepository.unlinkStudentParent(studentParentId); }

    @Override
    public void unlinkStudentFromParent(Long studentId, Long parentId) {
        studentParentRepository.unlinkByStudentAndParent(studentId, parentId);
    }

    @Override @Transactional(readOnly = true)
    public List<Parent> findByStudent(Long studentId) { return studentParentRepository.findParentsByStudentId(studentId); }

    @Override @Transactional(readOnly = true)
    public List<Student> findStudentsByParent(Long parentId) {
        return studentParentRepository.findStudentIdsByParentId(parentId).stream()
                .flatMap(id -> studentRepository.findById(id).stream()).toList();
    }
}
