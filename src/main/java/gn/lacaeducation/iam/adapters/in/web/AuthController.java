package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.LoginRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.LoginResponse;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Login, token refresh, password management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "Authenticate with email and password, returns JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var result = authUseCase.authenticate(new AuthUseCase.LoginCommand(req.email(), req.password()));
        return ResponseEntity.ok(new LoginResponse(
                result.token(), result.expiresIn(), result.userId(), result.tenantId(), result.roles()));
    }
}
