package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.iam.application.event.RoleAssignedEvent;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private UserService userService;

    @Test
    void create_duplicateEmailInTenant_throwsConflict() {
        when(userRepository.existsByTenantIdAndEmail(10L, "dup@ecole.gn")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new UserUseCase.CreateUserCommand(
                10L, "A", "B", "dup@ecole.gn", "pw", "0600")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_uniqueEmail_persists() {
        when(userRepository.existsByTenantIdAndEmail(10L, "new@ecole.gn")).thenReturn(false);
        when(passwordHasher.hash("pw")).thenReturn("$2a$10$h");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.create(new UserUseCase.CreateUserCommand(
                10L, "A", "B", "new@ecole.gn", "pw", "0600"));

        verify(userRepository).save(any(User.class));
        assert created.getPasswordHash().equals("$2a$10$h");
    }

    @Test
    void update_deactivate_revokesSessions() {
        User user = User.builder().id(5L).tenantId(10L).active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(5L, new UserUseCase.UpdateUserCommand("A", "B", "0600", false));

        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }

    @Test
    void update_activate_doesNotRevokeSessions() {
        User user = User.builder().id(5L).tenantId(10L).active(false).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(5L, new UserUseCase.UpdateUserCommand("A", "B", "0600", true));

        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void delete_softDeletesAndRevokesSessions() {
        User user = User.builder().id(5L).tenantId(10L).active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.delete(5L);

        org.assertj.core.api.Assertions.assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }

    @Test
    void update_partial_doesNotWipeUnsetFields() {
        User user = User.builder().id(5L).tenantId(10L).firstName("Mamadou").lastName("Diallo")
                .phone("0600").active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // deactivate carries only the active flag (all other fields null)
        userService.update(5L, new UserUseCase.UpdateUserCommand(null, null, null, false));

        org.assertj.core.api.Assertions.assertThat(user.getFirstName()).isEqualTo("Mamadou");
        org.assertj.core.api.Assertions.assertThat(user.getLastName()).isEqualTo("Diallo");
        org.assertj.core.api.Assertions.assertThat(user.getActive()).isFalse();
    }

    @Test
    void assignRole_publishesEvent() {
        userService.assignRole(5L, 2L);

        verify(userRepository).assignRole(5L, 2L);
        verify(eventPublisher).publishEvent(new RoleAssignedEvent(5L, 2L));
    }
}
