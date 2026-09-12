package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;

    @InjectMocks private TenantProvisioningService service;

    @Test
    void provisionTenantAdmin_createsSchoolAdminUserAndAssignsRole() {
        when(userRepository.existsByTenantIdAndEmail(10L, "admin@ecole.gn")).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn("$2a$10$h");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(99L); return u; });
        when(roleRepository.findByCode(RoleCode.SCHOOL_ADMIN))
                .thenReturn(Optional.of(Role.builder().id(2L).code(RoleCode.SCHOOL_ADMIN).build()));

        service.provisionTenantAdmin(10L, "École Test", "admin@ecole.gn", "Mamadou Diallo", "Kdrp-4827-Muxs", true, null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(10L);
        assertThat(saved.getEmail()).isEqualTo("admin@ecole.gn");
        assertThat(saved.getFirstName()).isEqualTo("Mamadou");
        assertThat(saved.getLastName()).isEqualTo("Diallo");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.isMustChangePassword()).isTrue();
        verify(userRepository).assignRole(99L, 2L);
    }

    @Test
    void provisionTenantAdmin_usesChosenPassword_whenProvided() {
        when(userRepository.existsByTenantIdAndEmail(10L, "admin@ecole.gn")).thenReturn(false);
        when(passwordHasher.hash("Secret123")).thenReturn("$2a$10$chosen");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(99L); return u; });
        when(roleRepository.findByCode(RoleCode.SCHOOL_ADMIN))
                .thenReturn(Optional.of(Role.builder().id(2L).code(RoleCode.SCHOOL_ADMIN).build()));

        service.provisionTenantAdmin(10L, "École Test", "admin@ecole.gn", "Mamadou Diallo", "Secret123", false, null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$chosen");
        assertThat(saved.isMustChangePassword()).isFalse();
        verify(passwordHasher).hash("Secret123");
    }

    @Test
    void provisionTenantAdmin_idempotent_whenAdminAlreadyExists() {
        when(userRepository.existsByTenantIdAndEmail(10L, "admin@ecole.gn")).thenReturn(true);

        service.provisionTenantAdmin(10L, "École Test", "admin@ecole.gn", "Mamadou Diallo", null, true, null, null);

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).assignRole(any(), any());
    }

    @Test
    void revokeTenantSessions_revokesEveryTenantUsersTokens() {
        when(userRepository.findByTenantId(10L)).thenReturn(List.of(
                User.builder().id(1L).build(),
                User.builder().id(2L).build()));

        service.revokeTenantSessions(10L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(refreshTokenRepository).revokeAllByUserId(2L);
    }
}
