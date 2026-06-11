package gn.lacaeducation.iam.application.service;

import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.out.PasswordHasher;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.application.port.out.TokenGenerator;
import gn.lacaeducation.iam.application.port.out.UserRepository;
import gn.lacaeducation.iam.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordHasher passwordHasher;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResult authenticate(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordHasher.verify(command.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("Account disabled");
        }

        List<String> roles = roleRepository.findUserRoles(user.getId())
                .stream().map(r -> r.getCode().name()).toList();

        String token = tokenGenerator.generate(user.getId(), user.getTenantId(), roles);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new LoginResult(token, jwtExpiration, user.getId(), user.getTenantId(), roles);
    }
}
