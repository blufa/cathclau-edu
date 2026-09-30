package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.Parent;
import java.util.List;
import java.util.Optional;

public interface ParentRepository {
    Parent save(Parent parent);
    Optional<Parent> findById(Long id);
    Optional<Parent> findByUserId(Long userId);
    /** Contrairement à {@link #findByUserId} (suppose un userId unique — casse si des fiches Parent
     * dupliquées partagent le même compte, voir la garde équivalente côté appelants), renvoie TOUTES
     * les fiches partageant ce compte, sans lever d'exception. */
    List<Parent> findAllByUserId(Long userId);
    Optional<Parent> findByIdempotencyKey(String idempotencyKey);
    List<Parent> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
