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
        return findChildStudentIds(userId).contains(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findChildStudentIds(Long userId) {
        if (userId == null) return java.util.List.of();
        // findAllByUserId (jamais findByUserId, qui suppose un userId unique) : un même compte peut se
        // retrouver sur PLUSIEURS fiches Parent (ex. lié séparément à chaque enfant lors de plusieurs
        // inscriptions "Personnel de l'école" au lieu de réutiliser la même fiche — bug de saisie vécu
        // en vrai, voir aussi la fiche de duplication à corriger côté création). findByUserId plantait
        // ici avec "Query did not return a unique result" dès qu'un parent avait 2+ fiches dupliquées —
        // bloquant, entre autres, toute génération de fiche de paie pour ce salarié (retenue scolarité).
        return parentRepository.findAllByUserId(userId).stream()
                .flatMap(parent -> studentParentRepository.findStudentIdsByParentId(parent.getId()).stream())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findChildStudentIdsWithValidatedEnrollment(Long userId) {
        return filterValidatedEnrollment(findChildStudentIds(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Long> findChildStudentIdsWithValidatedEnrollmentByStaffLink(String staffType, Long staffId) {
        if (staffType == null || staffId == null) return java.util.List.of();
        gn.lacaeducation.students.domain.StaffLinkType type;
        try {
            type = gn.lacaeducation.students.domain.StaffLinkType.valueOf(staffType);
        } catch (IllegalArgumentException e) {
            return java.util.List.of();
        }
        java.util.List<Long> studentIds = parentRepository.findAllByLinkedStaff(type, staffId).stream()
                .flatMap(parent -> studentParentRepository.findStudentIdsByParentId(parent.getId()).stream())
                .distinct()
                .toList();
        return filterValidatedEnrollment(studentIds);
    }

    private java.util.List<Long> filterValidatedEnrollment(java.util.List<Long> studentIds) {
        return studentIds.stream()
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

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DirectStaffLink> findDirectStaffLinksByStudent(Long studentId) {
        if (studentId == null) return java.util.List.of();
        return studentParentRepository.findParentsByStudentId(studentId).stream()
                .filter(parent -> parent.getLinkedStaffType() != null && parent.getLinkedStaffId() != null)
                .map(parent -> new DirectStaffLink(parent.getLinkedStaffType().name(), parent.getLinkedStaffId()))
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DirectStaffLink> findDirectStaffLinkByParentId(Long parentId) {
        if (parentId == null) return java.util.Optional.empty();
        return parentRepository.findById(parentId)
                .filter(parent -> parent.getLinkedStaffType() != null && parent.getLinkedStaffId() != null)
                .map(parent -> new DirectStaffLink(parent.getLinkedStaffType().name(), parent.getLinkedStaffId()));
    }
}
