package gn.lacaeducation.staff.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TeacherAvailabilityJpaRepository extends JpaRepository<TeacherAvailabilityJpaEntity, Long> {
    List<TeacherAvailabilityJpaEntity> findByTeacherIdOrderByDayAscStartTimeAsc(Long teacherId);
}
