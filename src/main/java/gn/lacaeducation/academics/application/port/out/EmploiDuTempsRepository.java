package gn.lacaeducation.academics.application.port.out;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
public interface EmploiDuTempsRepository {
    EmploiDuTemps save(EmploiDuTemps schedule);
    Optional<EmploiDuTemps> findById(Long id);
    List<EmploiDuTemps> findByClassId(Long classId);
    List<EmploiDuTemps> findByTeacherId(Long teacherId);
    List<EmploiDuTemps> search(Long classId, Long teacherId, Long roomId, JourSemaine day);
    void deleteById(Long id);
    boolean existsRoomConflict(Long roomId, JourSemaine day, LocalTime startTime, LocalTime endTime);
    boolean existsTeacherConflict(Long teacherId, JourSemaine day, LocalTime startTime, LocalTime endTime);
    boolean existsClassConflict(Long classId, JourSemaine day, LocalTime startTime, LocalTime endTime);
    boolean existsRoomConflictExcluding(Long roomId, JourSemaine day, LocalTime startTime, LocalTime endTime, Long excludeId);
    boolean existsTeacherConflictExcluding(Long teacherId, JourSemaine day, LocalTime startTime, LocalTime endTime, Long excludeId);
    boolean existsClassConflictExcluding(Long classId, JourSemaine day, LocalTime startTime, LocalTime endTime, Long excludeId);
}
