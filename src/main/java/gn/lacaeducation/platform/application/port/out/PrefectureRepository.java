package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.Prefecture;

import java.util.List;
import java.util.Optional;

public interface PrefectureRepository {
    Prefecture save(Prefecture prefecture);
    Optional<Prefecture> findById(Long id);
    List<Prefecture> findAll();

    /** Affecte un compte DPE_ADMIN à une préfecture/commune (idempotent). */
    void assignUser(Long userId, Long prefectureId);
    void unassignUser(Long userId, Long prefectureId);
    List<Long> findPrefectureIdsByUserId(Long userId);
}
