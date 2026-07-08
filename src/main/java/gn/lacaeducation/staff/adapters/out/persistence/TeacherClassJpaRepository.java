package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TeacherClassJpaRepository extends JpaRepository<TeacherClassJpaEntity, Long> {
    List<TeacherClassJpaEntity> findByTeacherId(Long teacherId);
    void deleteByTeacherId(Long teacherId);
}
