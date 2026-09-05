package gn.lacaeducation.courses.application.service;

import gn.lacaeducation.academics.AcademicYearPublicInfo;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.CourseBillingApi;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.courses.application.port.in.CoursePurchaseUseCase;
import gn.lacaeducation.courses.application.port.out.CoursePurchaseRepository;
import gn.lacaeducation.courses.application.port.out.CourseRepository;
import gn.lacaeducation.courses.application.port.out.CourseResourceRepository;
import gn.lacaeducation.courses.domain.Course;
import gn.lacaeducation.courses.domain.CoursePurchase;
import gn.lacaeducation.courses.domain.CoursePurchaseStatus;
import gn.lacaeducation.courses.domain.CourseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Achat d'un cours par un élève : émet une facture normale (frais miroir OTHER du cours), réglée via
 * le circuit de paiement scolaire existant (Djomy). L'accès est débloqué par
 * {@link CoursePurchasePaymentListener} dès que la facture est intégralement soldée.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CoursePurchaseService implements CoursePurchaseUseCase {

    private final CoursePurchaseRepository purchaseRepository;
    private final CourseRepository courseRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseBillingApi courseBillingApi;
    private final AcademicsDataProvider academicsDataProvider;

    @Override
    public CoursePurchase purchase(PurchaseCommand c) {
        Course course = courseRepository.findById(c.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", c.courseId()));
        if (!course.isPublished()) {
            throw new BusinessRuleException("Ce cours n'est pas disponible à l'achat.");
        }
        var existing = purchaseRepository.findByStudentIdAndCourseId(c.studentId(), c.courseId());
        if (existing.isPresent()) {
            // Déjà payé, ou facture déjà émise (le parent reprend le paiement sur la même facture) :
            // idempotent, on ne duplique jamais la facture d'achat.
            return existing.get();
        }
        AcademicYearPublicInfo year = academicsDataProvider.currentAcademicYear(c.tenantId())
                .orElseThrow(() -> new BusinessRuleException("Aucune année scolaire courante définie."));
        Long invoiceId = courseBillingApi.issueCourseInvoice(
                c.tenantId(), c.studentId(), course.getSchoolFeeId(), year.id(), course.getPrice());
        CoursePurchase purchase = CoursePurchase.builder()
                .tenantId(c.tenantId()).studentId(c.studentId()).courseId(c.courseId())
                .invoiceId(invoiceId).status(CoursePurchaseStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return purchaseRepository.save(purchase);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long studentId, Long courseId) {
        return purchaseRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(p -> p.getStatus() == CoursePurchaseStatus.PAID)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoursePurchase> findByStudentId(Long studentId) {
        return purchaseRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoursePurchase> findByTenantId(Long tenantId) {
        return purchaseRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResource> accessibleResources(Long studentId, Long courseId) {
        List<CourseResource> resources = resourceRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        if (hasAccess(studentId, courseId)) {
            return resources;
        }
        // Aperçu verrouillé : titre/type/durée visibles pour donner envie d'acheter, mais aucune URL.
        return resources.stream()
                .map(r -> CourseResource.builder()
                        .id(r.getId()).courseId(r.getCourseId()).type(r.getType()).title(r.getTitle())
                        .fileUrl(null).sortOrder(r.getSortOrder()).durationSeconds(r.getDurationSeconds())
                        .build())
                .toList();
    }
}
