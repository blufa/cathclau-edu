package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
interface EmployeeAttendanceJpaRepository extends JpaRepository<EmployeeAttendanceJpaEntity, Long> {
    Optional<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<EmployeeAttendanceJpaEntity> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetweenAndStatus(Long employeeId, LocalDate from, LocalDate to, EmployeeAttendanceStatus status);
}
