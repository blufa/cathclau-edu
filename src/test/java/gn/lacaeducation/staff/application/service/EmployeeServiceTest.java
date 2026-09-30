package gn.lacaeducation.staff.application.service;

import gn.lacaeducation.staff.application.port.in.EmployeeUseCase.UpdateEmployeeCommand;
import gn.lacaeducation.staff.application.port.out.EmployeeRepository;
import gn.lacaeducation.staff.domain.Employee;
import gn.lacaeducation.staff.domain.EmployeeCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock EmployeeRepository repo;
    @InjectMocks EmployeeService service;

    @Test
    void update_withUserId_linksAccount() {
        // Cas réel : personnel créé sans cocher « Créer un compte de connexion », compte créé après
        // coup — doit pouvoir être lié à sa fiche existante (sinon invisible à StaffSearchController).
        Employee existing = Employee.builder().id(1L).tenantId(10L).firstName("Awa").lastName("Diallo")
                .category(EmployeeCategory.ADMINISTRATIVE).userId(null).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Employee result = service.update(1L, new UpdateEmployeeCommand(null, "Awa", "Diallo", null, null,
                EmployeeCategory.ADMINISTRATIVE, null, null, null, null, null, null, 99L));

        assertThat(result.getUserId()).isEqualTo(99L);
    }

    @Test
    void update_withNullUserId_neverClearsExistingLink() {
        Employee existing = Employee.builder().id(1L).tenantId(10L).firstName("Awa").lastName("Diallo")
                .category(EmployeeCategory.ADMINISTRATIVE).userId(42L).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Employee result = service.update(1L, new UpdateEmployeeCommand(null, "Awa", "Diallo", null, null,
                EmployeeCategory.ADMINISTRATIVE, null, null, null, null, null, null, null));

        assertThat(result.getUserId()).isEqualTo(42L);
    }
}
