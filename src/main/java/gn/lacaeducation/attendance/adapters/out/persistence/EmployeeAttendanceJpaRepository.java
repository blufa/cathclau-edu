package gn.lacaeducation.attendance.adapters.out.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
interface EmployeeAttendanceJpaRepository extends JpaRepository<EmployeeAttendanceJpaEntity, Long> {
    List<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /** Verrou pessimiste sur la ligne la plus RÉCENTE du jour, sans filtrer sur l'état de sortie —
     * voir {@code EmployeeAttendanceRepository#lockLatestByEmployeeIdAndDate}, même raisonnement que
     * {@code StudentCheckinJpaRepository#lockOpenByStudentIdAndCheckinDate}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM EmployeeAttendanceJpaEntity a WHERE a.employeeId = :employeeId AND a.date = :date ORDER BY a.id DESC")
    List<EmployeeAttendanceJpaEntity> lockLatestByEmployeeIdAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    List<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
}
