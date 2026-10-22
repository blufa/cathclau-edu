package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.StudentCheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentCheckinRepository {
    StudentCheckin save(StudentCheckin c);
    Optional<StudentCheckin> findByStudentIdAndDateAndSlot(Long studentId, LocalDate date, Long scheduleSlotId);
    /** Comme {@link #findByStudentIdAndDateAndSlot}, mais pose un verrou pessimiste sur la ligne (si
     * elle existe) — sérialise deux scans de sortie quasi simultanés du même badge (§15/§16 : aucune
     * contrainte d'unicité ne protège une mise à jour, contrairement à l'insertion de l'entrée). */
    Optional<StudentCheckin> lockByStudentIdAndDateAndSlot(Long studentId, LocalDate date, Long scheduleSlotId);
    List<StudentCheckin> findByStudentIdAndDate(Long studentId, LocalDate date);
    List<StudentCheckin> findByClassIdAndDate(Long classId, LocalDate date);
}
