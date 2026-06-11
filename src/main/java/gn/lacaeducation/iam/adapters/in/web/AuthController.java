package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.LoginRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.LoginResponse;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Login, token refresh, password management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "Authenticate with email and password, returns access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var result = authUseCase.authenticate(new AuthUseCase.LoginCommand(req.email(), req.password()));
        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(summary = "Obtain a new access token using a refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        var result = authUseCase.refreshToken(req.refreshToken());
        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(summary = "Logout: revoke the refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest req) {
        authUseCase.logout(req.refreshToken());
    }

    @Operation(summary = "Request a password reset link (sent by email)")
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authUseCase.forgotPassword(req.email());
        return ResponseEntity.ok(Map.of("message", "Reset link sent if the account exists"));
    }

    @Operation(summary = "Reset password using a valid reset token")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authUseCase.resetPassword(new AuthUseCase.ResetPasswordCommand(req.token(), req.newPassword()));
    }

    @Operation(summary = "Change password for the authenticated user")
    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest req) {
        Long userId = Long.parseLong(jwt.getSubject());
        authUseCase.changePassword(new AuthUseCase.ChangePasswordCommand(userId,
                req.currentPassword(), req.newPassword()));
    }

    // DTO records
    record RefreshTokenRequest(@NotBlank String refreshToken) {}

    record ForgotPasswordRequest(@NotBlank @Email String email) {}

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    private LoginResponse toResponse(AuthUseCase.LoginResult result) {
        return new LoginResponse(result.accessToken(), result.refreshToken(),
                result.expiresIn(), result.userId(), result.tenantId(), result.roles());
    }
}
