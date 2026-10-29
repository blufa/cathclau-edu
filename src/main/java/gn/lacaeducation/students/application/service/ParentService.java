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
        // Un compte utilisateur (ex. personnel/enseignant lié comme parent) ne doit porter qu'une seule
        // fiche Parent : sans ce contrôle, inscrire plusieurs enfants du même personnel crée une fiche
        // dupliquée à chaque enfant (bug vécu en réel — voir findByUserId/findAllByUserId).
        if (cmd.userId() != null) {
            List<Parent> existing = parentRepository.findAllByUserId(cmd.userId());
            Optional<Parent> sameTenant = existing.stream()
                    .filter(p -> java.util.Objects.equals(p.getTenantId(), cmd.tenantId()))
                    .min(java.util.Comparator.comparing(Parent::getId));
            if (sameTenant.isPresent()) {
                return sameTenant.get();
            }
        }
        // Même garde anti-duplication pour le personnel lié DIRECTEMENT (sans compte) : inscrire
        // plusieurs enfants du même enseignant/employé sans compte ne doit pas créer une fiche par
        // enfant (même bug réel que pour userId, voir findAllByLinkedStaff).
        if (cmd.staffType() != null && cmd.staffId() != null) {
            Optional<Parent> sameTenant = parentRepository.findAllByLinkedStaff(cmd.staffType(), cmd.staffId()).stream()
                    .filter(p -> java.util.Objects.equals(p.getTenantId(), cmd.tenantId()))
                    .min(java.util.Comparator.comparing(Parent::getId));
            if (sameTenant.isPresent()) {
                return sameTenant.get();
            }
        }
        // Barrière anti-doublon par numéro de téléphone (§retour comptable, incident réel) : une
        // coupure réseau pendant l'inscription d'un parent (à l'inscription d'un enfant) peut laisser
        // croire à l'agent que la création a échoué alors qu'elle a réussi côté serveur — il
        // ressaisit alors le même parent, créant une fiche en double. Plutôt qu'une erreur bloquante,
        // on renvoie la fiche déjà existante (même principe que les gardes userId/staffId ci-dessus) :
        // le flux d'inscription peut enchaîner normalement sur le lien parent-enfant sans que l'agent
        // ait besoin de nettoyer un doublon après coup. Teste toutes les variantes de format du numéro
        // (voir PhoneNumbers), un numéro ressaisi dans un format différent doit rester détecté.
        if (cmd.phone() != null && !cmd.phone().isBlank()) {
            for (String candidate : gn.lacaeducation.common.PhoneNumbers.candidates(cmd.phone())) {
                Optional<Parent> existing = parentRepository.findByTenantIdAndPhone(cmd.tenantId(), candidate);
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
        }
        return parentRepository.save(Parent.builder()
                .tenantId(cmd.tenantId()).userId(cmd.userId())
                .firstName(PersonName.capitalizeFirstName(cmd.firstName()))
                .lastName(PersonName.upperCaseLastName(cmd.lastName()))
                .phone(cmd.phone()).email(cmd.email())
                .occupation(cmd.occupation()).address(cmd.address()).idempotencyKey(cmd.idempotencyKey())
                .linkedStaffType(cmd.staffType()).linkedStaffId(cmd.staffId()).build());
    }

    @Override
    public Parent update(Long id, UpdateParentCommand cmd) {
        Parent p = parentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + id));
        p.setFirstName(PersonName.capitalizeFirstName(cmd.firstName()));
        p.setLastName(PersonName.upperCaseLastName(cmd.lastName()));
        p.setPhone(cmd.phone());
        p.setEmail(cmd.email()); p.setOccupation(cmd.occupation()); p.setAddress(cmd.address());
        // Ne lie un compte/un personnel que s'il est fourni — ne jamais délier via une simple mise à
        // jour de profil (voir javadoc de UpdateParentCommand#userId).
        if (cmd.userId() != null) p.setUserId(cmd.userId());
        if (cmd.staffType() != null && cmd.staffId() != null) {
            p.setLinkedStaffType(cmd.staffType());
            p.setLinkedStaffId(cmd.staffId());
        }
        p.setExcludeFromAutoDeduction(cmd.excludeFromAutoDeduction());
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
        return findByUserId(userId)
                .orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("Parent (compte)", userId));
    }

    @Override @Transactional(readOnly = true)
    public Optional<Parent> findById(Long id) { return parentRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public Optional<Parent> findByUserId(Long userId) {
        // findAllByUserId (jamais findByUserId côté port, qui plante si des fiches Parent dupliquées
        // partagent le même compte — cas réel : une fiche recréée à chaque inscription d'enfant au lieu
        // de réutiliser l'existante). On retient la plus ancienne (id le plus bas) comme fiche "canonique"
        // pour le profil/portail parent, en attendant une fusion des doublons.
        return parentRepository.findAllByUserId(userId).stream()
                .min(java.util.Comparator.comparing(Parent::getId));
    }

    @Override @Transactional(readOnly = true)
    public List<Parent> findByTenantId(Long tenantId) { return parentRepository.findByTenantId(tenantId); }

    @Override
    public void delete(Long id) {
        // Purge D'ABORD les liens parent-enfant de cette fiche (jamais les fiches Student elles-mêmes)
        // — sans ça, supprimer un parent dupliqué (§retour comptable) laisse des liens orphelins
        // pointant vers un parent_id inexistant (pas de contrainte FK en base sur student_parents).
        studentParentRepository.unlinkAllByParentId(id);
        parentRepository.deleteById(id);
    }

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

    @Override @Transactional(readOnly = true)
    public List<Student> findStudentsByUserId(Long userId) {
        return parentRepository.findAllByUserId(userId).stream()
                .flatMap(p -> studentParentRepository.findStudentIdsByParentId(p.getId()).stream())
                .distinct()
                .flatMap(id -> studentRepository.findById(id).stream())
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public List<Long> findLinkedParentIds(Long tenantId) {
        var tenantParentIds = parentRepository.findByTenantId(tenantId).stream().map(Parent::getId)
                .collect(java.util.stream.Collectors.toSet());
        return studentParentRepository.findDistinctLinkedParentIds().stream()
                .filter(tenantParentIds::contains)
                .toList();
    }
}
