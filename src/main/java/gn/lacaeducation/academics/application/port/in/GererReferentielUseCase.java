package gn.lacaeducation.academics.application.port.in;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import gn.lacaeducation.academics.domain.valueobject.TypeSalle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface GererReferentielUseCase {
    // Room
    Salle createRoom(CreateRoomCommand command);
    Salle updateRoom(Long id, CreateRoomCommand command);
    Salle setRoomAvailability(Long id, boolean available);
    Optional<Salle> findRoomById(Long id);
    List<Salle> findRooms(Long tenantId);
    void deleteRoom(Long id);
    // Level
    Niveau createLevel(Long tenantId, String label, Integer sortOrder);
    Niveau updateLevel(Long id, String label, Integer sortOrder);
    Optional<Niveau> findLevelById(Long id);
    List<Niveau> findLevels(Long tenantId);
    void deleteLevel(Long id);
    // Subject
    Matiere createSubject(Long tenantId, String label, BigDecimal coefficient, String syllabus);
    Matiere updateSubject(Long id, String label, BigDecimal coefficient, String syllabus);
    Optional<Matiere> findSubjectById(Long id);
    List<Matiere> findSubjects(Long tenantId);
    void deleteSubject(Long id);
    record CreateRoomCommand(Long tenantId, String label, String building, Integer capacity, TypeSalle type) {}
}
