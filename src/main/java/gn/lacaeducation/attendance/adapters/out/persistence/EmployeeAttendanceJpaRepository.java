package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
interface EmployeeAttendanceJpaRepository extends JpaRepository<EmployeeAttendanceJpaEntity, Long> {
    Optional<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /** Verrou pessimiste (§15/§16) : voir {@code EmployeeAttendanceRepository#lockByEmployeeIdAndDate}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM EmployeeAttendanceJpaEntity a WHERE a.employeeId = :employeeId AND a.date = :date")
    Optional<EmployeeAttendanceJpaEntity> lockByEmployeeIdAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    List<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetweenAndStatus(Long employeeId, LocalDate from, LocalDate to, EmployeeAttendanceStatus status);
}
