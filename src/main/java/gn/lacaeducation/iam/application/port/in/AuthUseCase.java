package gn.lacaeducation.iam.application.port.in;

import java.util.List;

public interface AuthUseCase {
    LoginResult authenticate(LoginCommand command);
    LoginResult refreshToken(String refreshToken);
    void logout(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordCommand command);
    void changePassword(ChangePasswordCommand command);
    MeResult me(Long userId);

    /** {@code identifier} est le username (comptes créés par l'école) OU l'email (self-service). */
    record LoginCommand(String identifier, String password) {}

    record MeResult(
        Long userId,
        Long tenantId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String photo,
        List<String> roles,
        List<String> permissions
    ) {}

    record LoginResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        Long tenantId,
        List<String> roles,
        boolean mustChangePassword
    ) {}

    record ResetPasswordCommand(String token, String newPassword) {}

    record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {}
}
