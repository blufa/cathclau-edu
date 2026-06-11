package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.adapters.out.persistence.mapper.IamMapper;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        rolePermissionJpaRepository.deleteByRoleId(roleId);
        for (Long permId : permissionIds) {
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
