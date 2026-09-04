package gn.lacaeducation.dpe.adapters.out.persistence;

import gn.lacaeducation.dpe.adapters.out.persistence.mapper.DpeMapper;
import gn.lacaeducation.dpe.application.port.out.DpeAlertRepository;
import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class DpeAlertPersistenceAdapter implements DpeAlertRepository {

    private final DpeAlertJpaRepository jpa;
    private final DpeMapper mapper;

    @Override
    public DpeAlert save(DpeAlert alert) {
        return mapper.toDomain(jpa.save(mapper.toEntity(alert)));
    }

    @Override
    public Optional<DpeAlert> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DpeAlert> findByPrefectureIdAndTenantIdAndType(Long prefectureId, Long tenantId, DpeAlertType type) {
        return jpa.findByPrefectureIdAndTenantIdAndType(prefectureId, tenantId, type).map(mapper::toDomain);
    }

    @Override
    public List<DpeAlert> findByPrefectureId(Long prefectureId) {
        return jpa.findByPrefectureId(prefectureId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DpeAlert> findByPrefectureIdAndStatut(Long prefectureId, DpeAlertStatus statut) {
        return jpa.findByPrefectureIdAndStatut(prefectureId, statut).stream().map(mapper::toDomain).toList();
    }
}
