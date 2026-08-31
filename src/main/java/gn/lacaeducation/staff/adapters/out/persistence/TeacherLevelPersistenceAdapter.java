package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class TeacherLevelPersistenceAdapter implements TeacherLevelRepository {

    private final TeacherLevelJpaRepository jpa;

    @Override
    public List<Long> findLevelIds(Long teacherId) {
        return jpa.findByTeacherId(teacherId).stream().map(TeacherLevelJpaEntity::getLevelId).toList();
    }

    @Override
    public List<Long> findTeacherIds(Long levelId) {
        return jpa.findByLevelId(levelId).stream().map(TeacherLevelJpaEntity::getTeacherId).toList();
    }

    @Override
    @Transactional
    public void replace(Long teacherId, Long tenantId, List<Long> levelIds) {
        // Diff contre l'existant (voir RolePersistenceAdapter / TeacherSubjectPersistenceAdapter pour le
        // même correctif) : jamais de delete-then-insert sur une paire (teacher_id, level_id) déjà
        // présente, sous peine de violer la contrainte unique au flush.
        Set<Long> requested = levelIds == null ? Set.of() : new LinkedHashSet<>(levelIds);
        List<TeacherLevelJpaEntity> existing = jpa.findByTeacherId(teacherId);

        List<TeacherLevelJpaEntity> toRemove = existing.stream()
                .filter(tl -> !requested.contains(tl.getLevelId()))
                .toList();
        Set<Long> existingLevelIds = existing.stream().map(TeacherLevelJpaEntity::getLevelId).collect(Collectors.toSet());

        jpa.deleteAll(toRemove);
        for (Long levelId : requested) {
            if (existingLevelIds.contains(levelId)) continue;
            TeacherLevelJpaEntity e = new TeacherLevelJpaEntity();
            e.setTenantId(tenantId);
            e.setTeacherId(teacherId);
            e.setLevelId(levelId);
            jpa.save(e);
        }
    }
}
