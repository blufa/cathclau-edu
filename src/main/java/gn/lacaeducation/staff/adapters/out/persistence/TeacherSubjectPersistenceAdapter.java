package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class TeacherSubjectPersistenceAdapter implements TeacherSubjectRepository {

    private final TeacherSubjectJpaRepository jpa;

    @Override
    public List<Long> findSubjectIds(Long teacherId) {
        return jpa.findByTeacherId(teacherId).stream().map(TeacherSubjectJpaEntity::getSubjectId).toList();
    }

    @Override
    public List<Long> findTeacherIds(Long subjectId) {
        return jpa.findBySubjectId(subjectId).stream().map(TeacherSubjectJpaEntity::getTeacherId).toList();
    }

    @Override
    @Transactional
    public void replace(Long teacherId, Long tenantId, List<Long> subjectIds) {
        jpa.deleteByTeacherId(teacherId);
        if (subjectIds == null) return;
        subjectIds.stream().distinct().forEach(subjectId -> {
            TeacherSubjectJpaEntity e = new TeacherSubjectJpaEntity();
            e.setTenantId(tenantId);
            e.setTeacherId(teacherId);
            e.setSubjectId(subjectId);
            jpa.save(e);
        });
    }
}
