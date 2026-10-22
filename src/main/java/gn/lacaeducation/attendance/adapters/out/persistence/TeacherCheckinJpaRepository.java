package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface TeacherCheckinJpaRepository extends JpaRepository<TeacherCheckinJpaEntity, Long> {
    Optional<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDateAndScheduleSlotId(Long teacherId, LocalDate date, Long scheduleSlotId);

    /** Verrou pessimiste (§15/§16) : voir {@code TeacherCheckinRepository#lockByTeacherIdAndDateAndSlot}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM TeacherCheckinJpaEntity c WHERE c.teacherId = :teacherId AND c.checkinDate = :date "
            + "AND c.scheduleSlotId = :slot")
    Optional<TeacherCheckinJpaEntity> lockByTeacherIdAndCheckinDateAndScheduleSlotId(
            @Param("teacherId") Long teacherId, @Param("date") LocalDate date, @Param("slot") Long scheduleSlotId);

    List<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDate(Long teacherId, LocalDate date);
    List<TeacherCheckinJpaEntity> findByTeacherIdAndCheckinDateBetween(Long teacherId, LocalDate from, LocalDate to);
}
