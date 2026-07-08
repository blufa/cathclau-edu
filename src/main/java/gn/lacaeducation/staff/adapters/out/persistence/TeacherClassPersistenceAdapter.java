package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class TeacherClassPersistenceAdapter implements TeacherClassRepository {

    private final TeacherClassJpaRepository jpa;

    @Override
    public List<Long> findClassIds(Long teacherId) {
        return jpa.findByTeacherId(teacherId).stream().map(TeacherClassJpaEntity::getClassId).toList();
    }

    @Override
    @Transactional
    public void replace(Long teacherId, Long tenantId, List<Long> classIds) {
        jpa.deleteByTeacherId(teacherId);
        if (classIds == null) return;
        classIds.stream().distinct().forEach(classId -> {
            TeacherClassJpaEntity e = new TeacherClassJpaEntity();
            e.setTenantId(tenantId);
            e.setTeacherId(teacherId);
            e.setClassId(classId);
            jpa.save(e);
        });
    }
}
