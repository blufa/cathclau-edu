package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface StudentCheckinJpaRepository extends JpaRepository<StudentCheckinJpaEntity, Long> {
    Optional<StudentCheckinJpaEntity> findByStudentIdAndCheckinDateAndScheduleSlotId(Long studentId, LocalDate date, Long scheduleSlotId);
    List<StudentCheckinJpaEntity> findByStudentIdAndCheckinDate(Long studentId, LocalDate date);
    List<StudentCheckinJpaEntity> findByClassIdAndCheckinDate(Long classId, LocalDate date);
}
