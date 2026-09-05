package gn.lacaeducation.courses.application.port.out;

import gn.lacaeducation.courses.domain.CoursePurchase;

import java.util.List;
import java.util.Optional;

public interface CoursePurchaseRepository {
    CoursePurchase save(CoursePurchase purchase);
    Optional<CoursePurchase> findById(Long id);
    Optional<CoursePurchase> findByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<CoursePurchase> findByInvoiceId(Long invoiceId);
    List<CoursePurchase> findByStudentId(Long studentId);
    List<CoursePurchase> findByTenantId(Long tenantId);
    boolean existsByCourseId(Long courseId);
}
