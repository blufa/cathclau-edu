package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.AnneeScolaireRepository;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AnneeScolairePersistenceAdapter implements AnneeScolaireRepository {

    private final AnneeScolaireJpaRepository anneeRepo;
    private final AcademicsMapper mapper;

    @Override
    public AnneeScolaire save(AnneeScolaire a) {
        return mapper.toDomain(anneeRepo.save(mapper.toEntity(a)));
    }

    @Override
    public Optional<AnneeScolaire> findById(Long id) {
        return anneeRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AnneeScolaire> findCurrent(Long tenantId) {
        return anneeRepo.findByTenantIdAndCurrentTrue(tenantId).map(mapper::toDomain);
    }

    @Override
    public List<AnneeScolaire> findByTenantId(Long tenantId) {
        return anneeRepo.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void removeCurrent(Long tenantId) {
        anneeRepo.removeCurrent(tenantId);
    }
}
