package gn.lacaeducation.attendance.adapters.out.persistence;
import gn.lacaeducation.attendance.domain.TeacherAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
interface TeacherAttendanceJpaRepository extends JpaRepository<TeacherAttendanceJpaEntity, Long> {
    List<TeacherAttendanceJpaEntity> findByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
    long countByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
    long countByTeacherIdAndDateBetweenAndStatus(Long teacherId, LocalDate from, LocalDate to, TeacherAttendanceStatus status);
}
