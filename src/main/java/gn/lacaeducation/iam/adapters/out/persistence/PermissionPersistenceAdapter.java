package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.domain.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class PermissionPersistenceAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final UserPermissionJpaRepository userPermissionJpaRepository;
    private final UserRevokedPermissionJpaRepository userRevokedPermissionJpaRepository;
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

    @Override
    public List<Permission> findDirectByUserId(Long userId) {
        List<Long> permissionIds = userPermissionJpaRepository.findByUserId(userId)
                .stream().map(UserPermissionJpaEntity::getPermissionId).toList();
        return jpaRepository.findAllById(permissionIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void assignDirectPermissions(Long userId, List<Long> permissionIds) {
        // Diff contre l'existant (voir RolePersistenceAdapter.assignPermissions) : un delete-then-insert
        // sur les mêmes paires (userId, permissionId) déjà présentes viole la contrainte unique, car
        // Hibernate exécute les INSERT avant les DELETE au flush.
        Set<Long> requested = new LinkedHashSet<>(permissionIds);
        List<UserPermissionJpaEntity> existing = userPermissionJpaRepository.findByUserId(userId);

        List<UserPermissionJpaEntity> toRemove = existing.stream()
                .filter(up -> !requested.contains(up.getPermissionId()))
                .toList();
        Set<Long> existingPermissionIds = existing.stream().map(UserPermissionJpaEntity::getPermissionId).collect(Collectors.toSet());

        userPermissionJpaRepository.deleteAll(toRemove);
        for (Long permId : requested) {
            if (existingPermissionIds.contains(permId)) continue;
            UserPermissionJpaEntity up = new UserPermissionJpaEntity();
            up.setUserId(userId);
            up.setPermissionId(permId);
            userPermissionJpaRepository.save(up);
        }
    }

    @Override
    public List<Permission> findRevokedByUserId(Long userId) {
        List<Long> permissionIds = userRevokedPermissionJpaRepository.findByUserId(userId)
                .stream().map(UserRevokedPermissionJpaEntity::getPermissionId).toList();
        return jpaRepository.findAllById(permissionIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void revokePermissions(Long userId, List<Long> permissionIds) {
        // Même pattern diff que assignDirectPermissions ci-dessus (delete-then-insert sur les mêmes
        // paires viole la contrainte unique si Hibernate ordonne les INSERT avant les DELETE au flush).
        Set<Long> requested = new LinkedHashSet<>(permissionIds);
        List<UserRevokedPermissionJpaEntity> existing = userRevokedPermissionJpaRepository.findByUserId(userId);

        List<UserRevokedPermissionJpaEntity> toRemove = existing.stream()
                .filter(rev -> !requested.contains(rev.getPermissionId()))
                .toList();
        Set<Long> existingPermissionIds = existing.stream().map(UserRevokedPermissionJpaEntity::getPermissionId).collect(Collectors.toSet());

        userRevokedPermissionJpaRepository.deleteAll(toRemove);
        for (Long permId : requested) {
            if (existingPermissionIds.contains(permId)) continue;
            UserRevokedPermissionJpaEntity rev = new UserRevokedPermissionJpaEntity();
            rev.setUserId(userId);
            rev.setPermissionId(permId);
            userRevokedPermissionJpaRepository.save(rev);
        }
    }
}
