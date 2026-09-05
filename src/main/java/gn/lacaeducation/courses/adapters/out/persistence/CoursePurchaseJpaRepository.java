package gn.lacaeducation.courses.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CoursePurchaseJpaRepository extends JpaRepository<CoursePurchaseJpaEntity, Long> {
    Optional<CoursePurchaseJpaEntity> findByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<CoursePurchaseJpaEntity> findByInvoiceId(Long invoiceId);
    List<CoursePurchaseJpaEntity> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<CoursePurchaseJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    boolean existsByCourseId(Long courseId);
}
