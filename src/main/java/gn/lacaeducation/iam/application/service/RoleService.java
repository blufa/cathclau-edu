package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.iam.application.event.PermissionsModifiedEvent;
import gn.lacaeducation.iam.application.event.UserPermissionsModifiedEvent;
import gn.lacaeducation.iam.application.port.in.RoleUseCase;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleService implements RoleUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final gn.lacaeducation.iam.application.port.out.RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    @Override
    public Role create(CreateRoleCommand command) {
        Role role = Role.builder()
                .name(command.name())
                .description(command.description())
                .build();
        return roleRepository.save(role);
    }

    @Override
    public Role update(Long id, UpdateRoleCommand command) {
        Role role = findById(id);
        if (role.getCode() != null) {
            throw new BusinessRuleException("System roles cannot be modified");
        }
        role.setName(command.name());
        role.setDescription(command.description());
        return roleRepository.save(role);
    }

    @Override
    public void delete(Long id) {
        Role role = findById(id);
        if (role.getCode() != null) {
            throw new BusinessRuleException("System roles cannot be deleted");
        }
        roleRepository.deleteById(id);
    }

    @Override
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role", roleId);
        }
        roleRepository.assignPermissions(roleId, permissionIds);
        eventPublisher.publishEvent(new PermissionsModifiedEvent(roleId));
    }

    @Override
    public void removeRoleFromUser(Long userId, Long roleId) {
        roleRepository.removeRoleFromUser(userId, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findPermissionsByRole(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findDirectPermissionsByUser(Long userId) {
        return permissionRepository.findDirectByUserId(userId);
    }

    @Override
    public void assignUserPermissions(Long userId, List<Long> permissionIds) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        permissionRepository.assignDirectPermissions(userId, permissionIds);
        eventPublisher.publishEvent(new UserPermissionsModifiedEvent(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findRevokedPermissionsByUser(Long userId) {
        return permissionRepository.findRevokedByUserId(userId);
    }

    @Override
    public void revokeUserPermissions(Long userId, List<Long> permissionIds) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        permissionRepository.revokePermissions(userId, permissionIds);
        // Une réduction de privilèges doit s'appliquer tout de suite, pas seulement à la prochaine
        // expiration du jeton d'accès — même précaution que pour un changement de mot de passe.
        refreshTokenRepository.revokeAllByUserId(userId);
        eventPublisher.publishEvent(new UserPermissionsModifiedEvent(userId));
    }
}
