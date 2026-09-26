package gn.lacaeducation.students.application.service;

import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.application.port.out.EnrollmentRepository;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentParentRepository;
import gn.lacaeducation.students.domain.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ParentAccessProviderAdapter implements ParentAccessProvider {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final EnrollmentRepository enrollmentRepository;

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
    public java.util.List<Long> findChildStudentIdsWithValidatedEnrollment(Long userId) {
        return findChildStudentIds(userId).stream()
                .filter(studentId -> enrollmentRepository.findByStudentId(studentId).stream()
                        .anyMatch(e -> e.getStatus() == EnrollmentStatus.VALIDATED))
                .toList();
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

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Long> findUserIdByParentId(Long parentId) {
        if (parentId == null) return java.util.Optional.empty();
        return parentRepository.findById(parentId).map(gn.lacaeducation.students.domain.Parent::getUserId);
    }
}
