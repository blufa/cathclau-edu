package gn.lacaeducation.attendance.application.port.out;
import gn.lacaeducation.attendance.domain.EmployeeAttendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface EmployeeAttendanceRepository {
    EmployeeAttendance save(EmployeeAttendance attendance);
    Optional<EmployeeAttendance> findById(Long id);
    /** Toutes les lignes du jour pour cet employé — PLUSIEURS possibles depuis le passage aux cycles
     * entrée/sortie illimités par jour (§retour terrain, voir {@code StudentCheckinRepository}, même
     * modèle). Jamais {@code Optional} : une requête Spring Data dérivée qui attend un résultat
     * unique lèverait une exception dès le 2e cycle de la journée. */
    List<EmployeeAttendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    /** Verrou pessimiste sur la ligne la plus RÉCENTE du jour (toute, ouverte ou close — le filtrage
     * sur l'état se fait côté service), même principe que
     * {@code StudentCheckinRepository#lockOpenByStudentIdAndDate} : sérialise deux scans de sortie
     * quasi simultanés du même badge, et sert aussi de point d'ancrage pour une ré-entrée (nouveau
     * cycle) après un cycle déjà clos. */
    Optional<EmployeeAttendance> lockLatestByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
    void deleteById(Long id);
}
