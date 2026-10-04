package gn.lacaeducation.audit.application.service;

import gn.lacaeducation.audit.application.port.in.AuditLogUseCase;
import gn.lacaeducation.audit.application.port.out.AuditLogRepository;
import gn.lacaeducation.audit.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService implements AuditLogUseCase {

    private final AuditLogRepository repository;

    @Override
    public AuditLog record(RecordAuditCommand c) {
        return repository.save(AuditLog.builder()
                .tenantId(c.tenantId())
                .utilisateurId(c.utilisateurId())
                .action(c.action())
                .ressource(c.ressource())
                .ressourceId(c.ressourceId())
                .details(c.details())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findForCurrentTenant(Pageable pageable) {
        return repository.findForCurrentTenant(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> searchForCurrentTenant(String action, Long utilisateurId, LocalDateTime from,
                                                  LocalDateTime to, Pageable pageable) {
        return repository.searchForCurrentTenant(action, utilisateurId, from, to, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findPlatformActions(Pageable pageable) {
        return repository.findPlatformActions(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
