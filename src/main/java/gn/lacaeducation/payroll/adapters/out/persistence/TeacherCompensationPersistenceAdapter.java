package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TeacherCompensationPersistenceAdapter implements TeacherCompensationRepository {
    private final TeacherCompensationJpaRepository jpa;

    @Override public TeacherCompensation save(TeacherCompensation c) { return toDomain(jpa.save(toEntity(c))); }
    @Override public Optional<TeacherCompensation> findByTeacherId(Long t) { return jpa.findByTeacherId(t).map(this::toDomain); }
    @Override public List<TeacherCompensation> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(this::toDomain).toList(); }

    private TeacherCompensation toDomain(TeacherCompensationJpaEntity e) {
        return TeacherCompensation.builder().id(e.getId()).tenantId(e.getTenantId()).teacherId(e.getTeacherId())
                .payMode(e.getPayMode()).hourlyRateNormal(e.getHourlyRateNormal()).hourlyRateExam(e.getHourlyRateExam())
                .fixedSalary(e.getFixedSalary()).paidDuringVacation(e.isPaidDuringVacation()).build();
    }
    private TeacherCompensationJpaEntity toEntity(TeacherCompensation c) {
        TeacherCompensationJpaEntity e = new TeacherCompensationJpaEntity();
        e.setId(c.getId()); e.setTenantId(c.getTenantId()); e.setTeacherId(c.getTeacherId());
        e.setPayMode(c.getPayMode() != null ? c.getPayMode() : PayMode.HOURLY);
        e.setHourlyRateNormal(c.getHourlyRateNormal() != null ? c.getHourlyRateNormal() : BigDecimal.ZERO);
        e.setHourlyRateExam(c.getHourlyRateExam() != null ? c.getHourlyRateExam() : BigDecimal.ZERO);
        e.setFixedSalary(c.getFixedSalary() != null ? c.getFixedSalary() : BigDecimal.ZERO);
        e.setPaidDuringVacation(c.isPaidDuringVacation());
        return e;
    }
}
