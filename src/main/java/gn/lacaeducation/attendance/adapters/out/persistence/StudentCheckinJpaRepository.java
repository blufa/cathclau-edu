package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface StudentCheckinJpaRepository extends JpaRepository<StudentCheckinJpaEntity, Long> {
    Optional<StudentCheckinJpaEntity> findByStudentIdAndCheckinDateAndScheduleSlotId(Long studentId, LocalDate date, Long scheduleSlotId);
    List<StudentCheckinJpaEntity> findByStudentIdAndCheckinDate(Long studentId, LocalDate date);
    List<StudentCheckinJpaEntity> findByClassIdAndCheckinDate(Long classId, LocalDate date);

    /** Verrou pessimiste (§15/§16) : voir {@code StudentCheckinRepository#lockByStudentIdAndDateAndSlot}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StudentCheckinJpaEntity c WHERE c.studentId = :studentId AND c.checkinDate = :date "
            + "AND c.scheduleSlotId = :slot")
    Optional<StudentCheckinJpaEntity> lockByStudentIdAndCheckinDateAndScheduleSlotId(
            @Param("studentId") Long studentId, @Param("date") LocalDate date, @Param("slot") Long scheduleSlotId);
}
