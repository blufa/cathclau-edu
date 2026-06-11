package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.TokenGenerator;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.Role;
import gn.lacaeducation.iam.domain.RoleCode;
import gn.lacaeducation.iam.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private AuthService authService;

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
                .build();
    }

    @Test
    void authenticate_correctPassword_returnsLoginResult() {
        Role schoolAdminRole = Role.builder()
                .id(2L).code(RoleCode.SCHOOL_ADMIN).name("School Administrator").build();

        when(userRepository.findByEmail("mamadou@ecole.gn"))
                .thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("secret123", activeUser.getPasswordHash()))
                .thenReturn(true);
        when(roleRepository.findUserRoles(1L))
                .thenReturn(List.of(schoolAdminRole));
        when(tokenGenerator.generate(1L, 10L, List.of("SCHOOL_ADMIN")))
                .thenReturn("jwt.token.value");
        when(userRepository.save(any(User.class)))
                .thenReturn(activeUser);

        AuthUseCase.LoginResult result = authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "secret123")
        );

        assertThat(result.token()).isEqualTo("jwt.token.value");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.tenantId()).isEqualTo(10L);
        assertThat(result.roles()).containsExactly("SCHOOL_ADMIN");
        assertThat(result.expiresIn()).isEqualTo(86400L);
    }

    @Test
    void authenticate_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmail("mamadou@ecole.gn"))
                .thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("wrong", activeUser.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("mamadou@ecole.gn", "wrong")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail("unknown@ecole.gn"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("unknown@ecole.gn", "anything")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_disabledAccount_throwsDisabledException() {
        User inactiveUser = User.builder()
                .id(2L)
                .tenantId(10L)
                .email("inactive@ecole.gn")
                .passwordHash("$2a$10$hash")
                .active(false)
                .build();

        when(userRepository.findByEmail("inactive@ecole.gn"))
                .thenReturn(Optional.of(inactiveUser));
        when(passwordHasher.verify("secret", inactiveUser.getPasswordHash()))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(
                new AuthUseCase.LoginCommand("inactive@ecole.gn", "secret")
        )).isInstanceOf(DisabledException.class);
    }
}
