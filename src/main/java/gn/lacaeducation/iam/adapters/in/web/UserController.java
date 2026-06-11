package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.CreateUserRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.UserResponse;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.application.port.in.UserUseCase.CreateUserCommand;
import gn.lacaeducation.iam.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/iam/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userUseCase.create(
                new CreateUserCommand(
                        request.tenantId(),
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.password(),
                        request.phone()
                )
        );
        return ResponseEntity
                .created(URI.create("/api/iam/users/" + user.getId()))
                .body(toResponse(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return userUseCase.findById(id)
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return userUseCase.findById(userId)
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','SCHOOL_ADMIN','DIRECTOR')")
    public ResponseEntity<List<UserResponse>> findByTenant(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = jwt.getClaim("tenantId");
        List<UserResponse> users = userUseCase.findByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getTenantId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhone(),
                u.getActive(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}
