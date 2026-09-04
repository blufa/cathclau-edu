package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.platform.application.port.in.PrefectureUseCase;
import gn.lacaeducation.platform.application.port.out.PrefectureRepository;
import gn.lacaeducation.platform.application.port.out.TenantRepository;
import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureStatus;
import gn.lacaeducation.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PrefectureService implements PrefectureUseCase {

    private final PrefectureRepository repository;
    private final TenantRepository tenantRepository;

    @Override
    public Prefecture create(PrefectureCommand c) {
        return repository.save(Prefecture.builder()
                .nom(c.nom())
                .type(c.type())
                .region(c.region())
                .pays(c.pays() == null || c.pays().isBlank() ? "Guinée" : c.pays())
                .latitude(c.latitude())
                .longitude(c.longitude())
                .statut(PrefectureStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public Prefecture update(Long id, PrefectureCommand c) {
        Prefecture p = requireById(id);
        p.setNom(c.nom());
        p.setType(c.type());
        p.setRegion(c.region());
        if (c.pays() != null && !c.pays().isBlank()) {
            p.setPays(c.pays());
        }
        p.setLatitude(c.latitude());
        p.setLongitude(c.longitude());
        return repository.save(p);
    }

    @Override
    public Prefecture archive(Long id) {
        Prefecture p = requireById(id);
        p.setStatut(PrefectureStatus.ARCHIVEE);
        return repository.save(p);
    }

    @Override
    public Prefecture reactivate(Long id) {
        Prefecture p = requireById(id);
        p.setStatut(PrefectureStatus.ACTIVE);
        return repository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prefecture> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Prefecture findById(Long id) {
        return requireById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> schoolsOf(Long prefectureId) {
        requireById(prefectureId);
        return tenantRepository.findByPrefectureIdIn(Set.of(prefectureId));
    }

    @Override
    public Tenant assignSchool(Long prefectureId, Long tenantId) {
        requireById(prefectureId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setPrefectureId(prefectureId);
        return tenantRepository.save(tenant);
    }

    @Override
    public Tenant unassignSchool(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setPrefectureId(null);
        return tenantRepository.save(tenant);
    }

    @Override
    public void assignUser(Long prefectureId, Long userId) {
        requireById(prefectureId);
        if (userId == null) {
            throw new BusinessRuleException("userId requis");
        }
        repository.assignUser(userId, prefectureId);
    }

    @Override
    public void unassignUser(Long prefectureId, Long userId) {
        repository.unassignUser(userId, prefectureId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> prefectureIdsOfUser(Long userId) {
        return repository.findPrefectureIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> userIdsOfPrefecture(Long prefectureId) {
        requireById(prefectureId);
        return repository.findUserIdsByPrefectureId(prefectureId);
    }

    private Prefecture requireById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Prefecture", id));
    }
}
