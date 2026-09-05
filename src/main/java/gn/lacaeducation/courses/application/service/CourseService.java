package gn.lacaeducation.courses.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.CourseBillingApi;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.courses.application.port.in.CourseUseCase;
import gn.lacaeducation.courses.application.port.out.CoursePurchaseRepository;
import gn.lacaeducation.courses.application.port.out.CourseRepository;
import gn.lacaeducation.courses.application.port.out.CourseResourceRepository;
import gn.lacaeducation.courses.domain.Course;
import gn.lacaeducation.courses.domain.CourseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestion du catalogue de cours payants (§16 B). La facturation/le paiement ne sont jamais
 * réimplémentés ici : chaque cours possède un frais miroir (type OTHER) créé via {@link
 * CourseBillingApi} — la seule porte d'entrée exposée par {@code billing}, jamais ses ports internes
 * ni ses types de domaine — réutilisé par {@link CoursePurchaseService} pour émettre une facture
 * normale, payable via le circuit Djomy existant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService implements CourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseResourceRepository resourceRepository;
    private final CoursePurchaseRepository purchaseRepository;
    private final CourseBillingApi courseBillingApi;
    private final AcademicsDataProvider academicsDataProvider;

    @Override
    public Course create(CreateCourseCommand c) {
        if (c.title() == null || c.title().isBlank()) {
            throw new BusinessRuleException("Le titre du cours est requis.");
        }
        if (c.classId() == null) {
            throw new BusinessRuleException("Le cours doit être rattaché à une classe.");
        }
        if (c.price() == null || c.price().signum() <= 0) {
            throw new BusinessRuleException("Le prix du cours doit être positif.");
        }
        String academicYear = academicsDataProvider.currentAcademicYearLabel(c.tenantId())
                .orElseThrow(() -> new BusinessRuleException("Aucune année scolaire courante définie."));
        Long feeId = courseBillingApi.createCourseFee(
                c.tenantId(), c.classId(), c.price(), academicYear, feeLabel(c.title()));
        Course course = Course.builder()
                .tenantId(c.tenantId()).classId(c.classId()).subjectLabel(c.subjectLabel())
                .title(c.title()).description(c.description()).price(c.price())
                .thumbnailUrl(c.thumbnailUrl()).schoolFeeId(feeId).published(false)
                .createdAt(LocalDateTime.now())
                .build();
        return courseRepository.save(course);
    }

    @Override
    public Course update(Long id, UpdateCourseCommand c) {
        Course course = load(id);
        if (c.subjectLabel() != null) course.setSubjectLabel(c.subjectLabel());
        if (c.title() != null && !c.title().isBlank()) course.setTitle(c.title());
        if (c.description() != null) course.setDescription(c.description());
        if (c.thumbnailUrl() != null) course.setThumbnailUrl(c.thumbnailUrl());
        if (c.price() != null) {
            if (c.price().signum() <= 0) {
                throw new BusinessRuleException("Le prix du cours doit être positif.");
            }
            course.setPrice(c.price());
            // Le frais miroir suit le nouveau prix : seuls les achats futurs sont concernés, les
            // factures déjà émises gardent leur montant figé (comportement standard de la facturation).
            courseBillingApi.updateCourseFeeAmount(course.getSchoolFeeId(), c.price(), feeLabel(course.getTitle()));
        }
        return courseRepository.save(course);
    }

    @Override
    public Course publish(Long id) {
        Course course = load(id);
        if (resourceRepository.countByCourseId(id) == 0) {
            throw new BusinessRuleException("Ajoutez au moins une ressource (vidéo ou PDF) avant de publier ce cours.");
        }
        course.setPublished(true);
        return courseRepository.save(course);
    }

    @Override
    public Course unpublish(Long id) {
        Course course = load(id);
        course.setPublished(false);
        return courseRepository.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findByTenantId(Long tenantId) {
        return courseRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findPublishedByTenantId(Long tenantId) {
        return courseRepository.findByTenantIdAndPublishedTrue(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findByClassId(Long classId) {
        return courseRepository.findByClassId(classId);
    }

    @Override
    public void delete(Long id) {
        if (purchaseRepository.existsByCourseId(id)) {
            throw new BusinessRuleException("Ce cours a déjà été acheté par au moins un élève ; il ne peut plus être supprimé (dépubliez-le).");
        }
        resourceRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
    }

    @Override
    public CourseResource addResource(Long courseId, AddResourceCommand c) {
        load(courseId);
        if (c.fileUrl() == null || c.fileUrl().isBlank()) {
            throw new BusinessRuleException("Le fichier de la ressource est requis.");
        }
        if (c.type() == null) {
            throw new BusinessRuleException("Le type de la ressource (VIDEO ou PDF) est requis.");
        }
        int order = c.sortOrder() != null ? c.sortOrder()
                : (int) resourceRepository.countByCourseId(courseId);
        CourseResource resource = CourseResource.builder()
                .courseId(courseId).type(c.type()).title(c.title()).fileUrl(c.fileUrl())
                .sortOrder(order).durationSeconds(c.durationSeconds())
                .build();
        return resourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResource> findResourcesByCourseId(Long courseId) {
        return resourceRepository.findByCourseIdOrderBySortOrderAsc(courseId);
    }

    @Override
    public void deleteResource(Long resourceId) {
        resourceRepository.deleteById(resourceId);
    }

    private String feeLabel(String title) {
        return "Cours : " + title;
    }

    private Course load(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }
}
