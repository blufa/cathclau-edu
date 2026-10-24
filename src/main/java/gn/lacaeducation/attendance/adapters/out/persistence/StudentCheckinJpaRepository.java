package gn.lacaeducation.attendance.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface StudentCheckinJpaRepository extends JpaRepository<StudentCheckinJpaEntity, Long> {
    Optional<StudentCheckinJpaEntity> findByStudentIdAndCheckinDateAndScheduleSlotId(Long studentId, LocalDate date, Long scheduleSlotId);
    List<StudentCheckinJpaEntity> findByStudentIdAndCheckinDate(Long studentId, LocalDate date);
    List<StudentCheckinJpaEntity> findByClassIdAndCheckinDate(Long classId, LocalDate date);

    /** Verrou pessimiste (§15/§16) : voir {@code StudentCheckinRepository#lockByStudentIdAndDateAndSlot}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StudentCheckinJpaEntity c WHERE c.studentId = :studentId AND c.checkinDate = :date "
            + "AND c.scheduleSlotId = :slot")
    Optional<StudentCheckinJpaEntity> lockByStudentIdAndCheckinDateAndScheduleSlotId(
            @Param("studentId") Long studentId, @Param("date") LocalDate date, @Param("slot") Long scheduleSlotId);

    /** Comme ci-dessus, mais sur le pointage le plus RÉCENT du jour (toutes séances confondues), sans
     * filtrer sur l'état de sortie — voir {@code StudentCheckinRepository#lockOpenByStudentIdAndDate}.
     * Filtrer "checkOutTime IS NULL" directement dans la requête casserait le cas de course entre deux
     * scans de sortie quasi simultanés : le perdant du verrou doit retrouver la ligne même devenue
     * "fermée" entre-temps par le gagnant, pour la renvoyer telle quelle au lieu d'une erreur "aucune
     * entrée trouvée" trompeuse — le filtrage sur l'état se fait APRÈS, dans le service appelant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StudentCheckinJpaEntity c WHERE c.studentId = :studentId AND c.checkinDate = :date "
            + "ORDER BY c.id DESC")
    List<StudentCheckinJpaEntity> lockOpenByStudentIdAndCheckinDate(
            @Param("studentId") Long studentId, @Param("date") LocalDate date);
}
