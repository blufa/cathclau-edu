package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.LoginRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.LoginResponse;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.in.AuthUseCase.LoginCommand;
import gn.lacaeducation.iam.application.port.in.AuthUseCase.LoginResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authUseCase.authenticate(
                new LoginCommand(request.email(), request.password())
        );
        return ResponseEntity.ok(new LoginResponse(
                result.token(),
                result.expiresIn(),
                result.userId(),
                result.tenantId(),
                result.roles()
        ));
    }
}
