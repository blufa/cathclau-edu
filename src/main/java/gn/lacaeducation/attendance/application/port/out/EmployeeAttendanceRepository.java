package gn.lacaeducation.attendance.application.port.out;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import gn.lacaeducation.attendance.domain.EmployeeAttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface EmployeeAttendanceRepository {
    EmployeeAttendance save(EmployeeAttendance attendance);
    Optional<EmployeeAttendance> findById(Long id);
    Optional<EmployeeAttendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    /** Comme {@link #findByEmployeeIdAndDate}, mais pose un verrou pessimiste sur la ligne (si elle
     * existe) — même usage que {@code StudentCheckinRepository#lockByStudentIdAndDateAndSlot} (§15/§16) :
     * sérialise deux scans de sortie quasi simultanés du même badge. */
    Optional<EmployeeAttendance> lockByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployeeIdAndDateBetweenAndStatus(Long employeeId, LocalDate from, LocalDate to, EmployeeAttendanceStatus status);
    void deleteById(Long id);
}
