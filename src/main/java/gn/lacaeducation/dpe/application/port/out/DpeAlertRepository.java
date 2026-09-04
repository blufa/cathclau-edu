package gn.lacaeducation.dpe.application.port.out;

import gn.lacaeducation.dpe.domain.DpeAlert;
import gn.lacaeducation.dpe.domain.DpeAlertStatus;
import gn.lacaeducation.dpe.domain.DpeAlertType;

import java.util.List;
import java.util.Optional;

public interface DpeAlertRepository {
    DpeAlert save(DpeAlert alert);
    Optional<DpeAlert> findById(Long id);
    Optional<DpeAlert> findByPrefectureIdAndTenantIdAndType(Long prefectureId, Long tenantId, DpeAlertType type);
    List<DpeAlert> findByPrefectureId(Long prefectureId);
    List<DpeAlert> findByPrefectureIdAndStatut(Long prefectureId, DpeAlertStatus statut);
}
