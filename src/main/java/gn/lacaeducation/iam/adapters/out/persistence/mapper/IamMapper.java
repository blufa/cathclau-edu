package gn.lacaeducation.iam.adapters.out.persistence.mapper;

import gn.lacaeducation.iam.adapters.out.persistence.PermissionJpaEntity;
import gn.lacaeducation.iam.adapters.out.persistence.RoleJpaEntity;
import gn.lacaeducation.iam.adapters.out.persistence.UserJpaEntity;
import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IamMapper {

    // plainTemporaryPassword est un champ de transport (non persisté) : jamais lu depuis l'entité.
    @Mapping(target = "plainTemporaryPassword", ignore = true)
    User toDomain(UserJpaEntity entity);

    UserJpaEntity toEntity(User domain);

    Role toDomain(RoleJpaEntity entity);

    Permission toDomain(PermissionJpaEntity entity);
}
