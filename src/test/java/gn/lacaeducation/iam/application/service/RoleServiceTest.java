package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.iam.application.event.PermissionsModifiedEvent;
import gn.lacaeducation.iam.application.event.UserPermissionsModifiedEvent;
import gn.lacaeducation.iam.application.port.in.RoleUseCase;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private RoleService roleService;

    @Test
    void update_systemRole_throws() {
        Role system = Role.builder().id(1L).code(RoleCode.SCHOOL_ADMIN).name("School Administrator").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(system));

        assertThatThrownBy(() -> roleService.update(1L, new RoleUseCase.UpdateRoleCommand("x", "y")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void delete_systemRole_throws() {
        Role system = Role.builder().id(1L).code(RoleCode.TEACHER).name("Teacher").build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(system));

        assertThatThrownBy(() -> roleService.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void assignPermissions_publishesEvent() {
        when(roleRepository.existsById(3L)).thenReturn(true);

        roleService.assignPermissions(3L, List.of(1L, 2L));

        verify(roleRepository).assignPermissions(3L, List.of(1L, 2L));
        verify(eventPublisher).publishEvent(new PermissionsModifiedEvent(3L));
    }

    /** §retour comptable : réduire les privilèges d'un admin d'école précis doit s'appliquer tout de
     * suite, pas attendre l'expiration de son jeton — même précaution qu'un changement de mot de passe. */
    @Test
    void revokeUserPermissions_forcesReauth_andPublishesEvent() {
        when(userRepository.existsById(5L)).thenReturn(true);

        roleService.revokeUserPermissions(5L, List.of(1L, 2L));

        verify(permissionRepository).revokePermissions(5L, List.of(1L, 2L));
        verify(refreshTokenRepository).revokeAllByUserId(5L);
        verify(eventPublisher).publishEvent(new UserPermissionsModifiedEvent(5L));
    }

    @Test
    void revokeUserPermissions_unknownUser_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> roleService.revokeUserPermissions(99L, List.of(1L)))
                .isInstanceOf(gn.lacaeducation.common.ResourceNotFoundException.class);
    }
}
