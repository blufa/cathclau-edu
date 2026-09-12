package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.ReferentielRepository;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Salle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ReferentielPersistenceAdapter implements ReferentielRepository {

    private final SalleJpaRepository salleRepo;
    private final NiveauJpaRepository niveauRepo;
    private final MatiereJpaRepository matiereRepo;
    private final AcademicsMapper mapper;

    @Override public Salle saveRoom(Salle s) { return mapper.toDomain(salleRepo.save(mapper.toEntity(s))); }
    @Override public Optional<Salle> findRoomById(Long id) { return salleRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Salle> findRoomsByTenantId(Long t) { return salleRepo.findByTenantIdOrderById(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteRoomById(Long id) { salleRepo.deleteById(id); }
    @Override public Niveau saveLevel(Niveau n) { return mapper.toDomain(niveauRepo.save(mapper.toEntity(n))); }
    @Override public Optional<Niveau> findLevelById(Long id) { return niveauRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Niveau> findLevelsByTenantId(Long t) { return niveauRepo.findByTenantIdOrderBySortOrder(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteLevelById(Long id) { niveauRepo.deleteById(id); }
    @Override public Matiere saveSubject(Matiere m) { return mapper.toDomain(matiereRepo.save(mapper.toEntity(m))); }
    @Override public Optional<Matiere> findSubjectById(Long id) { return matiereRepo.findById(id).map(mapper::toDomain); }
    @Override public List<Matiere> findSubjectsByTenantId(Long t) { return matiereRepo.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteSubjectById(Long id) { matiereRepo.deleteById(id); }
}
