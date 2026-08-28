package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.StudentCheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentCheckinRepository {
    StudentCheckin save(StudentCheckin c);
    Optional<StudentCheckin> findByStudentIdAndDateAndSlot(Long studentId, LocalDate date, Long scheduleSlotId);
    List<StudentCheckin> findByStudentIdAndDate(Long studentId, LocalDate date);
    List<StudentCheckin> findByClassIdAndDate(Long classId, LocalDate date);
}
