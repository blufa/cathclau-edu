package gn.lacaeducation.academics.application.service;

import gn.lacaeducation.academics.application.port.in.GererReferentielUseCase;
import gn.lacaeducation.academics.application.port.out.ReferentielRepository;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReferentielAcademicsService implements GererReferentielUseCase {

    private final ReferentielRepository referentielRepository;

    @Override
    public Salle createRoom(CreateRoomCommand command) {
        Salle salle = Salle.builder()
                .tenantId(command.tenantId())
                .label(command.label())
                .building(command.building())
                .capacity(command.capacity())
                .type(command.type())
                .available(true)
                .build();
        return referentielRepository.saveRoom(salle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Salle> findRooms(Long tenantId) {
        return referentielRepository.findRoomsByTenantId(tenantId);
    }

    @Override
    public Niveau createLevel(Long tenantId, String label, Integer sortOrder) {
        return referentielRepository.saveLevel(
                Niveau.builder().tenantId(tenantId).label(label).sortOrder(sortOrder).build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Niveau> findLevels(Long tenantId) {
        return referentielRepository.findLevelsByTenantId(tenantId);
    }

    @Override
    public Matiere createSubject(Long tenantId, String label, BigDecimal coefficient, String syllabus) {
        return referentielRepository.saveSubject(
                Matiere.builder().tenantId(tenantId).label(label)
                        .coefficient(coefficient).syllabus(syllabus).build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Matiere> findSubjects(Long tenantId) {
        return referentielRepository.findSubjectsByTenantId(tenantId);
    }

    @Override
    public Salle updateRoom(Long id, CreateRoomCommand command) {
        Salle s = referentielRepository.findRoomById(id).orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
        s.setLabel(command.label()); s.setBuilding(command.building()); s.setCapacity(command.capacity()); s.setType(command.type());
        return referentielRepository.saveRoom(s);
    }
    @Override @Transactional(readOnly=true)
    public Optional<Salle> findRoomById(Long id) { return referentielRepository.findRoomById(id); }
    @Override public void deleteRoom(Long id) { referentielRepository.deleteRoomById(id); }

    @Override
    public Niveau updateLevel(Long id, String label, Integer sortOrder) {
        Niveau n = referentielRepository.findLevelById(id).orElseThrow(() -> new IllegalArgumentException("Level not found: " + id));
        n.setLabel(label); n.setSortOrder(sortOrder);
        return referentielRepository.saveLevel(n);
    }
    @Override @Transactional(readOnly=true)
    public Optional<Niveau> findLevelById(Long id) { return referentielRepository.findLevelById(id); }
    @Override public void deleteLevel(Long id) { referentielRepository.deleteLevelById(id); }

    @Override
    public Matiere updateSubject(Long id, String label, BigDecimal coefficient, String syllabus) {
        Matiere m = referentielRepository.findSubjectById(id).orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        m.setLabel(label); m.setCoefficient(coefficient); m.setSyllabus(syllabus);
        return referentielRepository.saveSubject(m);
    }
    @Override @Transactional(readOnly = true)
    public Optional<Matiere> findSubjectById(Long id) { return referentielRepository.findSubjectById(id); }
    @Override public void deleteSubject(Long id) { referentielRepository.deleteSubjectById(id); }
}
