package gn.lacaeducation.payroll.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TeacherVacationPayMonthJpaRepository extends JpaRepository<TeacherVacationPayMonthJpaEntity, Long> {
    List<TeacherVacationPayMonthJpaEntity> findByTeacherId(Long teacherId);
    boolean existsByTeacherIdAndYearAndMonth(Long teacherId, int year, int month);
}
