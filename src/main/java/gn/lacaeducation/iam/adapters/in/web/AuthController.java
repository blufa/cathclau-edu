package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.LoginRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.LoginResponse;
import gn.lacaeducation.iam.adapters.in.web.dto.MeResponse;
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

@Tag(name = "Authentication", description = "Connexion, renouvellement de token, gestion du mot de passe")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "S'authentifier avec identifiant ou e-mail + mot de passe, retourne l'access token et le refresh token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var result = authUseCase.authenticate(new AuthUseCase.LoginCommand(req.identifier(), req.password()));
        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(summary = "Obtenir un nouvel access token à partir d'un refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        var result = authUseCase.refreshToken(req.refreshToken());
        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(summary = "Déconnexion : révoquer le refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest req) {
        authUseCase.logout(req.refreshToken());
    }

    @Operation(summary = "Demander un lien de réinitialisation de mot de passe (envoyé par e-mail)")
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authUseCase.forgotPassword(req.email());
        return ResponseEntity.ok(Map.of("message", "Reset link sent if the account exists"));
    }

    @Operation(summary = "Réinitialiser le mot de passe avec un token de réinitialisation valide")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authUseCase.resetPassword(new AuthUseCase.ResetPasswordCommand(req.token(), req.newPassword()));
    }

    @Operation(summary = "Demander un code OTP de réinitialisation du mot de passe par SMS — identifiant : nom d'utilisateur, e-mail ou téléphone")
    @PostMapping("/password/otp/request")
    public ResponseEntity<Map<String, String>> requestPasswordResetOtp(@Valid @RequestBody OtpRequestRequest req) {
        authUseCase.requestPasswordResetOtp(req.identifier());
        return ResponseEntity.ok(Map.of("message", "Code envoyé par SMS si le compte existe"));
    }

    @Operation(summary = "Réinitialiser le mot de passe avec un code OTP valide reçu par SMS")
    @PostMapping("/password/otp/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPasswordWithOtp(@Valid @RequestBody OtpResetRequest req) {
        authUseCase.resetPasswordWithOtp(new AuthUseCase.ResetWithOtpCommand(req.identifier(), req.code(), req.newPassword()));
    }

    @Operation(summary = "Changer le mot de passe de l'utilisateur connecté")
    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest req) {
        Long userId = Long.parseLong(jwt.getSubject());
        authUseCase.changePassword(new AuthUseCase.ChangePasswordCommand(userId,
                req.currentPassword(), req.newPassword()));
    }

    @Operation(summary = "Profil de l'utilisateur connecté : rôles, permissions et tenant")
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        var r = authUseCase.me(userId);
        return new MeResponse(r.userId(), r.tenantId(), r.firstName(), r.lastName(),
                r.email(), r.phone(), r.photo(), r.roles(), r.permissions());
    }

    // DTO records
    record RefreshTokenRequest(@NotBlank String refreshToken) {}

    record ForgotPasswordRequest(@NotBlank @Email String email) {}

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    record OtpRequestRequest(@NotBlank String identifier) {}

    record OtpResetRequest(
            @NotBlank String identifier,
            @NotBlank String code,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8) String newPassword
    ) {}

    private LoginResponse toResponse(AuthUseCase.LoginResult result) {
        return new LoginResponse(result.accessToken(), result.refreshToken(),
                result.expiresIn(), result.userId(), result.tenantId(), result.roles(),
                result.mustChangePassword());
    }
}
