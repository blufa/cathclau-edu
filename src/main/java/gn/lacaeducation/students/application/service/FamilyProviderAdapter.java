package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.FamilyProvider;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.Parent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
class FamilyProviderAdapter implements FamilyProvider {

    private final StudentParentRepository studentParentRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChildInfo> findSiblings(Long studentId) {
        if (studentId == null) return List.of();
        // Tous les enfants rattachés aux parents de cet élève (union), l'élève lui-même inclus.
        Set<Long> childIds = new LinkedHashSet<>();
        childIds.add(studentId);
        for (Parent parent : studentParentRepository.findParentsByStudentId(studentId)) {
            childIds.addAll(studentParentRepository.findStudentIdsByParentId(parent.getId()));
        }
        return childIds.stream()
                .map(studentRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(s -> new ChildInfo(s.getId(), s.getBirthDate()))
                .toList();
    }
}
