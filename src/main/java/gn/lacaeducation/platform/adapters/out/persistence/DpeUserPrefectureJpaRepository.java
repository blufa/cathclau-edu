package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DpeUserPrefectureJpaRepository extends JpaRepository<DpeUserPrefectureJpaEntity, Long> {
    List<DpeUserPrefectureJpaEntity> findByUserId(Long userId);
    List<DpeUserPrefectureJpaEntity> findByPrefectureId(Long prefectureId);
    void deleteByUserIdAndPrefectureId(Long userId, Long prefectureId);
    boolean existsByUserIdAndPrefectureId(Long userId, Long prefectureId);
}
