package gn.lacaeducation.audit.adapters.out.persistence.mapper;

import gn.lacaeducation.audit.adapters.out.persistence.AuditLogJpaEntity;
import gn.lacaeducation.audit.domain.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    AuditLog toDomain(AuditLogJpaEntity entity);
    AuditLogJpaEntity toEntity(AuditLog domain);
}
