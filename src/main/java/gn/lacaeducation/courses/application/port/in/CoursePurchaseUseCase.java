package gn.lacaeducation.courses.application.port.in;

import gn.lacaeducation.courses.domain.CoursePurchase;
import gn.lacaeducation.courses.domain.CourseResource;

import java.util.List;

public interface CoursePurchaseUseCase {
    record PurchaseCommand(Long tenantId, Long courseId, Long studentId) {}

    /**
     * Émet (ou réutilise) la facture d'achat du cours pour cet élève — réutilise entièrement le
     * circuit de facturation/paiement scolaire (Djomy) : le paiement se fait ensuite via
     * {@code POST /api/v1/paiements} avec l'identifiant de facture renvoyé ici.
     */
    CoursePurchase purchase(PurchaseCommand command);
    boolean hasAccess(Long studentId, Long courseId);
    List<CoursePurchase> findByStudentId(Long studentId);
    List<CoursePurchase> findByTenantId(Long tenantId);

    /** Ressources du cours : URL incluse si l'élève a payé, sinon métadonnées seules (aperçu verrouillé). */
    List<CourseResource> accessibleResources(Long studentId, Long courseId);
}
