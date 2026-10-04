package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

/** Page frontend journal d'audit (§retour fondateurs, A3) : filtres optionnels action/utilisateur/période. */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock AuditLogRepository repository;
    @InjectMocks AuditLogService service;

    @Test
    void searchForCurrentTenant_delegatesAllFiltersToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 4, 23, 59);

        service.searchForCurrentTenant("CASHIER_SESSION_CLOSED", 99L, from, to, pageable);

        verify(repository).searchForCurrentTenant("CASHIER_SESSION_CLOSED", 99L, from, to, pageable);
    }
}
