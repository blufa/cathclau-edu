package gn.lacaeducation.iam.application.port.in;

import java.util.List;

public interface AuthUseCase {
    LoginResult authenticate(LoginCommand command);
    LoginResult refreshToken(String refreshToken);
    void logout(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordCommand command);
    void changePassword(ChangePasswordCommand command);

    record LoginCommand(String email, String password) {}

    record LoginResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        Long tenantId,
        List<String> roles
    ) {}

    record ResetPasswordCommand(String token, String newPassword) {}

    record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {}
}
