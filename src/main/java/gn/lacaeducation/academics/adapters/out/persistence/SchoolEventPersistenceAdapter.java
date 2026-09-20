package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.adapters.out.persistence.mapper.AcademicsMapper;
import gn.lacaeducation.academics.application.port.out.SchoolEventRepository;
import gn.lacaeducation.academics.domain.SchoolEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SchoolEventPersistenceAdapter implements SchoolEventRepository {

    private final SchoolEventJpaRepository jpaRepo;
    private final AcademicsMapper mapper;

    @Override
    public SchoolEvent save(SchoolEvent event) {
        return mapper.toDomain(jpaRepo.save(mapper.toEntity(event)));
    }

    @Override
    public Optional<SchoolEvent> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SchoolEvent> findByTenantId(Long tenantId) {
        return jpaRepo.findByTenantIdOrderByStartDateDesc(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SchoolEvent> findByTenantIdAndDateRangeOverlap(Long tenantId, LocalDate from, LocalDate to) {
        return jpaRepo.findByTenantIdAndDateRangeOverlap(tenantId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
