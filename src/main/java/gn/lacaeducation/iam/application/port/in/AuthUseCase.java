package gn.lacaeducation.iam.application.port.in;

import java.util.List;

public interface AuthUseCase {
    LoginResult authenticate(LoginCommand command);

    record LoginCommand(String email, String password) {}

    record LoginResult(
        String token,
        long expiresIn,
        Long userId,
        Long tenantId,
        List<String> roles
    ) {}
}
