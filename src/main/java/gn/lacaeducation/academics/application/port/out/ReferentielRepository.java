package gn.lacaeducation.academics.application.port.out;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import java.util.List;
import java.util.Optional;
public interface ReferentielRepository {
    Salle saveRoom(Salle room);
    Optional<Salle> findRoomById(Long id);
    List<Salle> findRoomsByTenantId(Long tenantId);
    void deleteRoomById(Long id);
    Niveau saveLevel(Niveau level);
    Optional<Niveau> findLevelById(Long id);
    List<Niveau> findLevelsByTenantId(Long tenantId);
    void deleteLevelById(Long id);
    Matiere saveSubject(Matiere subject);
    Optional<Matiere> findSubjectById(Long id);
    List<Matiere> findSubjectsByTenantId(Long tenantId);
    void deleteSubjectById(Long id);
}
