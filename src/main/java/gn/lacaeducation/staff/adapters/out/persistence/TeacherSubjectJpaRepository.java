package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TeacherSubjectJpaRepository extends JpaRepository<TeacherSubjectJpaEntity, Long> {
    List<TeacherSubjectJpaEntity> findByTeacherId(Long teacherId);
    List<TeacherSubjectJpaEntity> findBySubjectId(Long subjectId);
}
