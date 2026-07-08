package gn.lacaeducation.payroll.adapters.out.persistence.mapper;

import gn.lacaeducation.payroll.adapters.out.persistence.PayslipJpaEntity;
import gn.lacaeducation.payroll.adapters.out.persistence.PayslipLineJpaEntity;
import gn.lacaeducation.payroll.adapters.out.persistence.SalaryRuleJpaEntity;
import gn.lacaeducation.payroll.adapters.out.persistence.SalaryStructureJpaEntity;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipLine;
import gn.lacaeducation.payroll.domain.SalaryRule;
import gn.lacaeducation.payroll.domain.SalaryStructure;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayrollMapper {
    Payslip toDomain(PayslipJpaEntity e);
    PayslipJpaEntity toEntity(Payslip d);

    PayslipLine toDomain(PayslipLineJpaEntity e);
    PayslipLineJpaEntity toEntity(PayslipLine d);

    SalaryStructure toDomain(SalaryStructureJpaEntity e);
    SalaryStructureJpaEntity toEntity(SalaryStructure d);

    SalaryRule toDomain(SalaryRuleJpaEntity e);
    SalaryRuleJpaEntity toEntity(SalaryRule d);
}
