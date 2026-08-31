package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PrefectureRepository;
import gn.lacaeducation.platform.domain.Prefecture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PrefecturePersistenceAdapter implements PrefectureRepository {

    private final PrefectureJpaRepository jpa;
    private final DpeUserPrefectureJpaRepository dpeUserJpa;
    private final PlatformMapper mapper;

    @Override
    public Prefecture save(Prefecture prefecture) {
        return mapper.toDomain(jpa.save(mapper.toEntity(prefecture)));
    }

    @Override
    public Optional<Prefecture> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Prefecture> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void assignUser(Long userId, Long prefectureId) {
        if (dpeUserJpa.existsByUserIdAndPrefectureId(userId, prefectureId)) {
            return;
        }
        DpeUserPrefectureJpaEntity e = new DpeUserPrefectureJpaEntity();
        e.setUserId(userId);
        e.setPrefectureId(prefectureId);
        dpeUserJpa.save(e);
    }

    @Override
    public void unassignUser(Long userId, Long prefectureId) {
        dpeUserJpa.deleteByUserIdAndPrefectureId(userId, prefectureId);
    }

    @Override
    public List<Long> findPrefectureIdsByUserId(Long userId) {
        return dpeUserJpa.findByUserId(userId).stream().map(DpeUserPrefectureJpaEntity::getPrefectureId).toList();
    }
}
