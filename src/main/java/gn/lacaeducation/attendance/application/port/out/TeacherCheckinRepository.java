package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.TeacherCheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherCheckinRepository {
    TeacherCheckin save(TeacherCheckin c);
    Optional<TeacherCheckin> findByTeacherIdAndDateAndSlot(Long teacherId, LocalDate date, Long scheduleSlotId);
    /** Comme {@link #findByTeacherIdAndDateAndSlot}, mais pose un verrou pessimiste sur la ligne (si
     * elle existe) — même usage que {@code StudentCheckinRepository#lockByStudentIdAndDateAndSlot}
     * (§15/§16) : sérialise deux scans de sortie quasi simultanés du même badge. */
    Optional<TeacherCheckin> lockByTeacherIdAndDateAndSlot(Long teacherId, LocalDate date, Long scheduleSlotId);
    List<TeacherCheckin> findByTeacherIdAndDate(Long teacherId, LocalDate date);
    List<TeacherCheckin> findByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);
}
