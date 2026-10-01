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
    /** Fiches Parent déjà liées DIRECTEMENT (sans compte) à ce membre du personnel — même garde
     *  anti-duplication que {@link #findAllByUserId} (inscrire plusieurs enfants du même personnel
     *  sans compte ne doit pas créer une fiche Parent par enfant). */
    List<Parent> findAllByLinkedStaff(gn.lacaeducation.students.domain.StaffLinkType staffType, Long staffId);
    Optional<Parent> findByIdempotencyKey(String idempotencyKey);
    List<Parent> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
