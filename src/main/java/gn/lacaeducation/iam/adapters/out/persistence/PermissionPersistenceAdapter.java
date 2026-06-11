package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.domain.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PermissionPersistenceAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final IamMapper mapper;

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findByRoleId(Long roleId) {
        List<Long> permissionIds = rolePermissionJpaRepository.findByRoleId(roleId)
                .stream().map(RolePermissionJpaEntity::getPermissionId).toList();
        return jpaRepository.findAllById(permissionIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Permission> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }
}
