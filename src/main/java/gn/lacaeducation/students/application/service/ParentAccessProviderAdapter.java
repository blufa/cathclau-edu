package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ParentAccessProviderAdapter implements ParentAccessProvider {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isParentOfStudent(Long userId, Long studentId) {
        if (userId == null || studentId == null) return false;
        return parentRepository.findByUserId(userId)
                .map(parent -> studentParentRepository.findStudentIdsByParentId(parent.getId()).contains(studentId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findChildStudentIds(Long userId) {
        if (userId == null) return java.util.List.of();
        return parentRepository.findByUserId(userId)
                .map(parent -> studentParentRepository.findStudentIdsByParentId(parent.getId()))
                .orElseGet(java.util.List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findParentUserIdsByStudent(Long studentId) {
        if (studentId == null) return java.util.List.of();
        return studentParentRepository.findParentsByStudentId(studentId).stream()
                .map(gn.lacaeducation.students.domain.Parent::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
