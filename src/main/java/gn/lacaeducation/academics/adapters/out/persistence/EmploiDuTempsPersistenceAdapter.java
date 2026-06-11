package gn.lacaeducation.academics.adapters.out.persistence;
import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.EmploiDuTempsRepository;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class EmploiDuTempsPersistenceAdapter implements EmploiDuTempsRepository {
    private final EmploiDuTempsJpaRepository edtRepo;
    private final AcademicsMapper mapper;
    @Override public EmploiDuTemps save(EmploiDuTemps e) { return mapper.toDomain(edtRepo.save(mapper.toEntity(e))); }
    @Override public Optional<EmploiDuTemps> findById(Long id) { return edtRepo.findById(id).map(mapper::toDomain); }
    @Override public List<EmploiDuTemps> findByClassId(Long classId) { return edtRepo.findBySchoolClassId(classId).stream().map(mapper::toDomain).toList(); }
    @Override public List<EmploiDuTemps> findByTeacherId(Long teacherId) { return edtRepo.findByTeacherId(teacherId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { edtRepo.deleteById(id); }
    @Override public boolean existsRoomConflict(Long roomId, JourSemaine day, LocalTime st, LocalTime et) { return edtRepo.existsRoomConflict(roomId, day, st, et); }
}
