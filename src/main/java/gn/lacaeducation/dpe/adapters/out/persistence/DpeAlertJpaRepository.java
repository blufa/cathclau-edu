package gn.lacaeducation.dpe.adapters.out.persistence;

import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface DpeAlertJpaRepository extends JpaRepository<DpeAlertJpaEntity, Long> {
    Optional<DpeAlertJpaEntity> findByPrefectureIdAndTenantIdAndType(Long prefectureId, Long tenantId, DpeAlertType type);
    List<DpeAlertJpaEntity> findByPrefectureId(Long prefectureId);
    List<DpeAlertJpaEntity> findByPrefectureIdAndStatut(Long prefectureId, DpeAlertStatus statut);
}
