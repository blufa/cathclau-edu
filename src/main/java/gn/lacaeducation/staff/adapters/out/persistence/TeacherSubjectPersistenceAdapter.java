package gn.lacaeducation.staff.adapters.out.persistence;

import gn.lacaeducation.staff.application.port.out.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        // Diff contre l'existant plutôt que "tout supprimer puis tout réinsérer" : un delete-then-insert
        // sur les mêmes paires (teacher_id, subject_id) déjà présentes viole la contrainte unique de la
        // table, car Hibernate exécute les INSERT avant les DELETE au flush (voir RolePersistenceAdapter
        // pour le même correctif appliqué au même bug sur les permissions de rôle).
        Set<Long> requested = subjectIds == null ? Set.of() : new LinkedHashSet<>(subjectIds);
        List<TeacherSubjectJpaEntity> existing = jpa.findByTeacherId(teacherId);

        List<TeacherSubjectJpaEntity> toRemove = existing.stream()
                .filter(ts -> !requested.contains(ts.getSubjectId()))
                .toList();
        Set<Long> existingSubjectIds = existing.stream().map(TeacherSubjectJpaEntity::getSubjectId).collect(Collectors.toSet());

        jpa.deleteAll(toRemove);
        for (Long subjectId : requested) {
            if (existingSubjectIds.contains(subjectId)) continue;
            TeacherSubjectJpaEntity e = new TeacherSubjectJpaEntity();
            e.setTenantId(tenantId);
            e.setTeacherId(teacherId);
            e.setSubjectId(subjectId);
            jpa.save(e);
        }
    }
}
