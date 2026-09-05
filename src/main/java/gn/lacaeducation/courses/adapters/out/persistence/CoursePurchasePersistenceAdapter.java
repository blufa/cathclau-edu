package gn.lacaeducation.courses.adapters.out.persistence;

import gn.lacaeducation.courses.application.port.out.CoursePurchaseRepository;
import gn.lacaeducation.courses.domain.CoursePurchase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CoursePurchasePersistenceAdapter implements CoursePurchaseRepository {

    private final CoursePurchaseJpaRepository jpa;

    @Override
    public CoursePurchase save(CoursePurchase p) {
        return toDomain(jpa.save(toEntity(p)));
    }

    @Override
    public Optional<CoursePurchase> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CoursePurchase> findByStudentIdAndCourseId(Long studentId, Long courseId) {
        return jpa.findByStudentIdAndCourseId(studentId, courseId).map(this::toDomain);
    }

    @Override
    public Optional<CoursePurchase> findByInvoiceId(Long invoiceId) {
        return jpa.findByInvoiceId(invoiceId).map(this::toDomain);
    }

    @Override
    public List<CoursePurchase> findByStudentId(Long studentId) {
        return jpa.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<CoursePurchase> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByCourseId(Long courseId) {
        return jpa.existsByCourseId(courseId);
    }

    private CoursePurchase toDomain(CoursePurchaseJpaEntity e) {
        return CoursePurchase.builder()
                .id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId()).courseId(e.getCourseId())
                .invoiceId(e.getInvoiceId()).status(e.getStatus())
                .createdAt(e.getCreatedAt()).purchasedAt(e.getPurchasedAt())
                .build();
    }

    private CoursePurchaseJpaEntity toEntity(CoursePurchase d) {
        CoursePurchaseJpaEntity e = new CoursePurchaseJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setStudentId(d.getStudentId());
        e.setCourseId(d.getCourseId()); e.setInvoiceId(d.getInvoiceId()); e.setStatus(d.getStatus());
        e.setCreatedAt(d.getCreatedAt()); e.setPurchasedAt(d.getPurchasedAt());
        return e;
    }
}
