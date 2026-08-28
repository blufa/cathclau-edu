package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
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
class RolePersistenceAdapter implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final RolePermissionJpaRepository rolePermissionJpaRepository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final IamMapper mapper;

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Role> findUserRoles(Long userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity;
        if (role.getId() != null) {
            entity = jpaRepository.findById(role.getId()).orElse(new RoleJpaEntity());
        } else {
            entity = new RoleJpaEntity();
        }
        entity.setName(role.getName());
        entity.setCode(role.getCode());
        entity.setDescription(role.getDescription());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // Diff contre l'existant plutôt que "tout supprimer puis tout réinsérer" : un delete-then-insert
        // sur les MÊMES paires (roleId, permissionId) déjà présentes provoque une violation de contrainte
        // unique, car Hibernate exécute les INSERT avant les DELETE au flush (ordre d'action par défaut),
        // donc l'INSERT s'exécute alors que l'ancienne ligne existe encore en base.
        Set<Long> requested = new LinkedHashSet<>(permissionIds);
        List<RolePermissionJpaEntity> existing = rolePermissionJpaRepository.findByRoleId(roleId);

        List<RolePermissionJpaEntity> toRemove = existing.stream()
                .filter(rp -> !requested.contains(rp.getPermissionId()))
                .toList();
        Set<Long> existingPermissionIds = existing.stream().map(RolePermissionJpaEntity::getPermissionId).collect(Collectors.toSet());

        rolePermissionJpaRepository.deleteAll(toRemove);
        for (Long permId : requested) {
            if (existingPermissionIds.contains(permId)) continue;
            RolePermissionJpaEntity rp = new RolePermissionJpaEntity();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionJpaRepository.save(rp);
        }
    }

    @Override
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleJpaRepository.deleteByUserIdAndRoleId(userId, roleId);
    }
}
