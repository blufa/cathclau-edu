package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.iam.application.event.PasswordResetEvent;
import gn.lacaeducation.iam.application.event.UserLoggedInEvent;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.PasswordResetTokenRepository;
import gn.lacaeducation.iam.application.port.out.PermissionRepository;
import gn.lacaeducation.iam.application.port.out.RefreshTokenRepository;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.TokenGenerator;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.PasswordResetToken;
import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.RefreshToken;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import gn.lacaeducation.platform.TenantInfoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private PasswordHasher passwordHasher;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private TenantInfoProvider tenantInfoProvider;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<Object> eventCaptor;

    @InjectMocks private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400L);

        activeUser = User.builder()
                .id(1L)
                .tenantId(10L)
                .lastName("Diallo")
                .firstName("Mamadou")
                .email("mamadou@ecole.gn")
                .passwordHash("$2a$10$hashedpassword")
                .active(true)
                .failedAttempts(0)
                .build();
    }

    @Test
    void authenticate_correctPassword_returnsResultWithRolesAndPublishesEvent() {
        Role schoolAdmin = Role.builder().id(2L).code(RoleCode.SCHOOL_ADMIN).name("School Administrator").build();
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("secret123", activeUser.getPasswordHash())).thenReturn(true);
        when(tenantInfoProvider.isTenantActive(10L)).thenReturn(true);
        when(roleRepository.findUserRoles(1L)).thenReturn(List.of(schoolAdmin));
        when(permissionRepository.findByUserId(1L))
                .thenReturn(List.of(Permission.builder().id(5L).code("user:create").build()));
        when(tokenGenerator.generate(eq(1L), eq(10L), eq(List.of("SCHOOL_ADMIN")), eq(List.of("user:create"))))
                .thenReturn("jwt.token.value");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        AuthUseCase.LoginResult result = authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "secret123"));

        assertThat(result.accessToken()).isEqualTo("jwt.token.value");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.tenantId()).isEqualTo(10L);
        assertThat(result.roles()).containsExactly("SCHOOL_ADMIN");
        assertThat(result.expiresIn()).isEqualTo(86400L);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(UserLoggedInEvent.class);
    }

    @Test
    void authenticate_wrongPassword_incrementsAttempts_throwsBadCredentials() {
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("wrong", activeUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(activeUser.getFailedAttempts()).isEqualTo(1);
        assertThat(activeUser.getLockedUntil()).isNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    void authenticate_fifthFailure_locksAccount() {
        activeUser.setFailedAttempts(4);
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("wrong", activeUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(activeUser.getFailedAttempts()).isEqualTo(5);
        assertThat(activeUser.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void authenticate_lockedAccount_throwsBusinessRule() {
        activeUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "secret123")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void authenticate_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail("unknown@ecole.gn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("unknown@ecole.gn", "anything")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_disabledAccount_throwsDisabledException() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "secret")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void authenticate_suspendedTenant_throwsDisabledException() {
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("secret123", activeUser.getPasswordHash())).thenReturn(true);
        when(tenantInfoProvider.isTenantActive(10L)).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "secret123")))
                .isInstanceOf(DisabledException.class);

        verify(eventPublisher, never()).publishEvent(any(UserLoggedInEvent.class));
    }

    @Test
    void refreshToken_revoked_throws() {
        RefreshToken revoked = RefreshToken.builder().userId(1L).token("rt")
                .expiresAt(LocalDateTime.now().plusDays(1)).revoked(true).build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refreshToken("rt"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void refreshToken_expired_throws() {
        RefreshToken expired = RefreshToken.builder().userId(1L).token("rt")
                .expiresAt(LocalDateTime.now().minusDays(1)).revoked(false).build();
        when(refreshTokenRepository.findByToken("rt")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refreshToken("rt"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void refreshToken_valid_rotatesAndRevokesOld() {
        RefreshToken valid = RefreshToken.builder().userId(1L).token("old")
                .expiresAt(LocalDateTime.now().plusDays(1)).revoked(false).build();
        when(refreshTokenRepository.findByToken("old")).thenReturn(Optional.of(valid));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(tenantInfoProvider.isTenantActive(10L)).thenReturn(true);
        when(roleRepository.findUserRoles(1L)).thenReturn(List.of());
        when(permissionRepository.findByUserId(1L)).thenReturn(List.of());
        when(tokenGenerator.generate(any(), any(), anyList(), anyList())).thenReturn("new.jwt");

        AuthUseCase.LoginResult result = authService.refreshToken("old");

        assertThat(result.accessToken()).isEqualTo("new.jwt");
        verify(refreshTokenRepository).revokeByToken("old");
    }

    @Test
    void logout_revokesToken() {
        authService.logout("rt");
        verify(refreshTokenRepository).revokeByToken("rt");
    }

    @Test
    void resetPassword_validToken_updatesPasswordRevokesSessionsAndPublishesEvent() {
        PasswordResetToken token = PasswordResetToken.builder().userId(1L).token("reset")
                .expiresAt(LocalDateTime.now().plusHours(1)).used(false).build();
        when(passwordResetTokenRepository.findByToken("reset")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.hash("newpass123")).thenReturn("$2a$10$new");

        authService.resetPassword(new AuthUseCase.ResetPasswordCommand("reset", "newpass123"));

        assertThat(activeUser.getPasswordHash()).isEqualTo("$2a$10$new");
        assertThat(token.isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(eventPublisher).publishEvent(any(PasswordResetEvent.class));
    }

    @Test
    void resetPassword_usedToken_throws() {
        PasswordResetToken used = PasswordResetToken.builder().userId(1L).token("reset")
                .expiresAt(LocalDateTime.now().plusHours(1)).used(true).build();
        when(passwordResetTokenRepository.findByToken("reset")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> authService.resetPassword(
                new AuthUseCase.ResetPasswordCommand("reset", "newpass123")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("badcurrent", activeUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                new AuthUseCase.ChangePasswordCommand(1L, "badcurrent", "newpass123")))
                .isInstanceOf(BusinessRuleException.class);
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void changePassword_validCurrent_updatesAndRevokesSessions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("current", activeUser.getPasswordHash())).thenReturn(true);
        when(passwordHasher.hash("newpass123")).thenReturn("$2a$10$new");

        authService.changePassword(new AuthUseCase.ChangePasswordCommand(1L, "current", "newpass123"));

        assertThat(activeUser.getPasswordHash()).isEqualTo("$2a$10$new");
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void me_aggregatesRolesAndPermissions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findUserRoles(1L))
                .thenReturn(List.of(Role.builder().code(RoleCode.TEACHER).build()));
        when(permissionRepository.findByUserId(1L))
                .thenReturn(List.of(Permission.builder().code("note:read").build()));

        AuthUseCase.MeResult me = authService.me(1L);

        assertThat(me.email()).isEqualTo("mamadou@ecole.gn");
        assertThat(me.tenantId()).isEqualTo(10L);
        assertThat(me.roles()).containsExactly("TEACHER");
        assertThat(me.permissions()).containsExactly("note:read");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void forgotPassword_unknownEmail_doesNotLeakAndSavesNoToken() {
        when(userRepository.findByEmail("ghost@ecole.gn")).thenReturn(Optional.empty());

        authService.forgotPassword("ghost@ecole.gn");

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void forgotPassword_knownEmail_savesToken() {
        when(userRepository.findByEmail("mamadou@ecole.gn")).thenReturn(Optional.of(activeUser));

        authService.forgotPassword("mamadou@ecole.gn");

        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
    }
}
