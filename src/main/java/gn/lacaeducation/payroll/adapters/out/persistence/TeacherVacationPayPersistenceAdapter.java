package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.application.port.out.TeacherVacationPayRepository;
import gn.lacaeducation.payroll.domain.TeacherVacationPayMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TeacherVacationPayPersistenceAdapter implements TeacherVacationPayRepository {
    private final TeacherVacationPayMonthJpaRepository jpa;

    @Override
    public TeacherVacationPayMonth save(TeacherVacationPayMonth m) {
        TeacherVacationPayMonthJpaEntity e = new TeacherVacationPayMonthJpaEntity();
        e.setId(m.getId()); e.setTenantId(m.getTenantId()); e.setTeacherId(m.getTeacherId());
        e.setYear(m.getYear()); e.setMonth(m.getMonth());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<TeacherVacationPayMonth> findByTeacherId(Long teacherId) {
        return jpa.findByTeacherId(teacherId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByTeacherIdAndYearAndMonth(Long teacherId, int year, int month) {
        return jpa.existsByTeacherIdAndYearAndMonth(teacherId, year, month);
    }

    @Override
    public void deleteById(Long id) { jpa.deleteById(id); }

    private TeacherVacationPayMonth toDomain(TeacherVacationPayMonthJpaEntity e) {
        return TeacherVacationPayMonth.builder().id(e.getId()).tenantId(e.getTenantId())
                .teacherId(e.getTeacherId()).year(e.getYear()).month(e.getMonth()).build();
    }
}
