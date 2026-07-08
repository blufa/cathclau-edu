package gn.lacaeducation.calendar.adapters.out.persistence;

import gn.lacaeducation.calendar.adapters.out.persistence.mapper.AcademicEventMapper;
import gn.lacaeducation.calendar.application.port.out.AcademicEventRepository;
import gn.lacaeducation.calendar.domain.AcademicEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AcademicEventPersistenceAdapter implements AcademicEventRepository {
    private final AcademicEventJpaRepository jpa;
    private final AcademicEventMapper mapper;

    @Override public AcademicEvent save(AcademicEvent e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<AcademicEvent> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<AcademicEvent> findByTenantId(Long t) { return jpa.findByTenantIdOrderByStartDateAsc(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<AcademicEvent> findByTenantIdAndAcademicYearId(Long t, Long y) { return jpa.findByTenantIdAndAcademicYearIdOrderByStartDateAsc(t, y).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
