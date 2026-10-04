package gn.lacaeducation.audit.adapters.out.persistence;

import gn.lacaeducation.audit.adapters.out.persistence.mapper.AuditMapper;
import gn.lacaeducation.audit.application.port.out.AuditLogRepository;
import gn.lacaeducation.audit.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class AuditLogPersistenceAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;
    private final AuditMapper mapper;

    @Override
    public AuditLog save(AuditLog log) {
        return mapper.toDomain(jpa.save(mapper.toEntity(log)));
    }

    @Override
    public Page<AuditLog> findForCurrentTenant(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> searchForCurrentTenant(String action, Long utilisateurId, LocalDateTime from,
                                                  LocalDateTime to, Pageable pageable) {
        return jpa.search(action, utilisateurId, from, to, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findPlatformActions(Pageable pageable) {
        return jpa.findByTenantIdIsNull(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }
}
