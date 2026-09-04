package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.RecessWindowRepository;
import gn.lacaeducation.attendance.domain.RecessWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class RecessWindowPersistenceAdapter implements RecessWindowRepository {

    private final RecessWindowJpaRepository repo;

    @Override
    public List<RecessWindow> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public RecessWindow save(RecessWindow w) {
        RecessWindowJpaEntity e = new RecessWindowJpaEntity();
        e.setId(w.getId());
        e.setTenantId(w.getTenantId());
        e.setLabel(w.getLabel());
        e.setStartTime(w.getStartTime());
        e.setEndTime(w.getEndTime());
        return toDomain(repo.save(e));
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    private RecessWindow toDomain(RecessWindowJpaEntity e) {
        return RecessWindow.builder().id(e.getId()).tenantId(e.getTenantId())
                .label(e.getLabel()).startTime(e.getStartTime()).endTime(e.getEndTime()).build();
    }
}
