package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.PeriodeRepository;
import gn.lacaeducation.academics.domain.Periode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PeriodePersistenceAdapter implements PeriodeRepository {

    private final PeriodeJpaRepository periodeRepo;
    private final AcademicsMapper mapper;

    @Override
    public Periode save(Periode p) {
        return mapper.toDomain(periodeRepo.save(mapper.toEntity(p)));
    }

    @Override
    public Optional<Periode> findById(Long id) {
        return periodeRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Periode> findByAcademicYearId(Long academicYearId) {
        return periodeRepo.findByAcademicYearIdOrderBySortOrder(academicYearId).stream().map(mapper::toDomain).toList();
    }
}
