package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.common.MatriculeAllocator;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import gn.lacaeducation.staff.application.port.out.TeacherRepository;
import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Attribue un matricule aux fiches enseignant/personnel qui n'en ont encore aucun — soit parce
 * qu'elles ont été créées avant l'auto-génération (voir {@code EnseignantService}/{@code EmployeeService}),
 * soit via un import en masse qui n'en fournissait pas. Idempotent : ne touche que les fiches dont le
 * matricule est encore vide, donc ne fait plus rien dès que le rattrapage a eu lieu une fois par tenant
 * (même pattern que {@code SuperAdminBootstrap}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffMatriculeBackfill implements ApplicationRunner {

    private final TeacherRepository teacherRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantInfoProvider tenantInfoProvider;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        backfillTeachers();
        backfillEmployees();
    }

    private void backfillTeachers() {
        List<Teacher> missing = teacherRepository.findMissingStaffNumber();
        if (missing.isEmpty()) return;
        Map<Long, List<Teacher>> byTenant = missing.stream().collect(Collectors.groupingBy(Teacher::getTenantId));
        byTenant.forEach((tenantId, teachers) -> {
            long seq = teacherRepository.findByTenantId(tenantId).size() + 1L;
            for (Teacher t : teachers) {
                String candidate;
                do {
                    candidate = MatriculeAllocator.candidate(tenantInfoProvider, tenantId, seq++);
                } while (teacherRepository.existsByTenantIdAndStaffNumber(tenantId, candidate));
                t.setStaffNumber(candidate);
                teacherRepository.save(t);
            }
        });
        log.info("Rattrapage matricule enseignants : {} fiche(s) complétée(s)", missing.size());
    }

    private void backfillEmployees() {
        List<Employee> missing = employeeRepository.findMissingEmployeeNumber();
        if (missing.isEmpty()) return;
        Map<Long, List<Employee>> byTenant = missing.stream().collect(Collectors.groupingBy(Employee::getTenantId));
        byTenant.forEach((tenantId, employees) -> {
            long seq = employeeRepository.findByTenantId(tenantId).size() + 1L;
            for (Employee e : employees) {
                String candidate;
                do {
                    candidate = MatriculeAllocator.candidate(tenantInfoProvider, tenantId, seq++);
                } while (employeeRepository.existsByTenantIdAndEmployeeNumber(tenantId, candidate));
                e.setEmployeeNumber(candidate);
                employeeRepository.save(e);
            }
        });
        log.info("Rattrapage matricule personnel : {} fiche(s) complétée(s)", missing.size());
    }
}
