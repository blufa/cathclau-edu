package gn.lacaeducation.staff.adapters.out;

import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class EmployeePublicInfoProviderAdapter implements EmployeePublicInfoProvider {

    private final EmployeeRepository employeeRepository;

    @Override
    public List<EmployeeInfo> listEmployees(Long tenantId) {
        return employeeRepository.findByTenantId(tenantId).stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public Optional<EmployeeInfo> findEmployee(Long id) {
        return employeeRepository.findById(id).map(this::toInfo);
    }

    @Override
    public Optional<Long> findEmployeeIdByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return employeeRepository.findByUserId(userId).map(gn.lacaeducation.staff.domain.Employee::getId);
    }

    private EmployeeInfo toInfo(gn.lacaeducation.staff.domain.Employee e) {
        return new EmployeeInfo(e.getId(), e.fullName(),
                e.getCategory() != null ? e.getCategory().name() : null,
                e.getBaseSalary(), Boolean.TRUE.equals(e.getActive()), e.getPhone(), e.getEmail());
    }
}
