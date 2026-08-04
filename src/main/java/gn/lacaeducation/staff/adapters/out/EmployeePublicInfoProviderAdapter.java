package gn.lacaeducation.staff.adapters.out;

import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class EmployeePublicInfoProviderAdapter implements EmployeePublicInfoProvider {

    private final EmployeeRepository employeeRepository;

    @Override
    public List<EmployeeInfo> listEmployees(Long tenantId) {
        return employeeRepository.findByTenantId(tenantId).stream()
                .map(e -> new EmployeeInfo(e.getId(), e.fullName(),
                        e.getCategory() != null ? e.getCategory().name() : null,
                        e.getBaseSalary(), Boolean.TRUE.equals(e.getActive())))
                .toList();
    }
}
