package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SalaryAdvancePersistenceAdapter implements SalaryAdvanceRepository {

    private final SalaryAdvanceJpaRepository jpa;

    @Override
    public SalaryAdvance save(SalaryAdvance a) {
        return toDomain(jpa.save(toEntity(a)));
    }

    @Override
    public Optional<SalaryAdvance> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<SalaryAdvance> findByTenantId(Long tenantId) {
        return jpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<SalaryAdvance> findByTenantIdAndTeacherIdAndEmployeeType(Long tenantId, Long teacherId, EmployeeType employeeType) {
        return jpa.findByTenantIdAndTeacherIdAndEmployeeType(tenantId, teacherId, employeeType).stream()
                .map(this::toDomain).toList();
    }

    private SalaryAdvance toDomain(SalaryAdvanceJpaEntity e) {
        return SalaryAdvance.builder()
                .id(e.getId()).tenantId(e.getTenantId()).employeeType(e.getEmployeeType())
                .teacherId(e.getTeacherId()).teacherName(e.getTeacherName())
                .amount(e.getAmount()).remainingBalance(e.getRemainingBalance()).reason(e.getReason())
                .status(e.getStatus()).grantedAt(e.getGrantedAt()).lastDeductedAt(e.getLastDeductedAt())
                .grantedByUserId(e.getGrantedByUserId())
                .build();
    }

    private SalaryAdvanceJpaEntity toEntity(SalaryAdvance d) {
        SalaryAdvanceJpaEntity e = new SalaryAdvanceJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setEmployeeType(d.getEmployeeType());
        e.setTeacherId(d.getTeacherId()); e.setTeacherName(d.getTeacherName());
        e.setAmount(d.getAmount()); e.setRemainingBalance(d.getRemainingBalance()); e.setReason(d.getReason());
        e.setStatus(d.getStatus()); e.setGrantedAt(d.getGrantedAt()); e.setLastDeductedAt(d.getLastDeductedAt());
        e.setGrantedByUserId(d.getGrantedByUserId());
        return e;
    }
}
