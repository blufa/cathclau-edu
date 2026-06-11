package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
interface StudentAttendanceJpaRepository extends JpaRepository<StudentAttendanceJpaEntity, Long> {
    List<StudentAttendanceJpaEntity> findByClassIdAndDate(Long classId, LocalDate date);
    List<StudentAttendanceJpaEntity> findByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);
    List<StudentAttendanceJpaEntity> findByClassIdAndDateBetweenAndStatus(Long classId, LocalDate from, LocalDate to, AttendanceStatus status);
}
