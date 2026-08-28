package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TeacherLevelJpaRepository extends JpaRepository<TeacherLevelJpaEntity, Long> {
    List<TeacherLevelJpaEntity> findByTeacherId(Long teacherId);
    List<TeacherLevelJpaEntity> findByLevelId(Long levelId);
}
