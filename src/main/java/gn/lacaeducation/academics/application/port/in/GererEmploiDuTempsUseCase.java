package gn.lacaeducation.academics.application.port.in;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
public interface GererEmploiDuTempsUseCase {
    EmploiDuTemps create(CreateScheduleCommand command);
    EmploiDuTemps update(Long id, CreateScheduleCommand command);
    Optional<EmploiDuTemps> findById(Long id);
    List<EmploiDuTemps> findByClass(Long classId);
    List<EmploiDuTemps> findByTeacher(Long teacherId);
    List<EmploiDuTemps> search(Long classId, Long teacherId, Long roomId, JourSemaine day);
    void delete(Long id);
    record CreateScheduleCommand(Long tenantId, Long schoolClassId, Long subjectId, Long teacherId,
                                  Long roomId, JourSemaine day, LocalTime startTime, LocalTime endTime) {}
}
