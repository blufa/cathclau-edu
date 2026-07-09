package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface TeacherCheckinJpaRepository extends JpaRepository<TeacherCheckinJpaEntity, Long> {
    Optional<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDateAndScheduleSlotId(Long teacherId, LocalDate date, Long scheduleSlotId);
    List<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDate(Long teacherId, LocalDate date);
    List<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDateBetween(Long teacherId, LocalDate from, LocalDate to);
}
