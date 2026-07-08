package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.adapters.in.web.dto.CreateUserRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.UserResponse;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Users", description = "User management within a tenant")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @Operation(summary = "Create a new user in the current tenant")
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        // The tenant is taken from the authenticated admin's context so a tenant admin can only
        // create users inside their own tenant. SUPER_ADMIN (no tenant context) may target a tenant
        // explicitly via the request body.
        Long tenantId = TenantContext.get() != null ? TenantContext.get() : req.tenantId();
        var user = userUseCase.create(new UserUseCase.CreateUserCommand(
                tenantId, req.firstName(), req.lastName(), req.email(), req.password(), req.phone()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId()))
                .body(toResponse(user));
    }

    @Operation(summary = "List all users in the current tenant")
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PageResponse<UserResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                               @PageableDefault(size = 20) Pageable pageable) {
        Long tenantId = jwt.getClaim("tenant_id") instanceof Number n ? n.longValue() : null;
        List<UserResponse> list = userUseCase.findByTenantId(tenantId).stream().map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        // Hibernate's @Filter does not scope find-by-PK, so enforce tenant isolation here:
        // a tenant-scoped caller may only read users of their own tenant.
        Long ctx = TenantContext.get();
        return userUseCase.findById(id)
                .filter(u -> ctx == null || ctx.equals(u.getTenantId()))
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the profile of the authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return userUseCase.findById(userId).map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a user")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return toResponse(userUseCase.update(id, new UserUseCase.UpdateUserCommand(
                req.firstName(), req.lastName(), req.phone(), req.active())));
    }

    @Operation(summary = "Deactivate a user account")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        userUseCase.update(id, new UserUseCase.UpdateUserCommand(null, null, null, false));
    }

    @Operation(summary = "Reactivate a user account")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable Long id) {
        userUseCase.update(id, new UserUseCase.UpdateUserCommand(null, null, null, true));
    }

    @Operation(summary = "Delete (soft-delete) a user")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { userUseCase.delete(id); }

    @Operation(summary = "Assign a role to a user")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable Long id, @RequestParam Long roleId) {
        userUseCase.assignRole(id, roleId);
    }

    @Operation(summary = "Get roles assigned to a user")
    @GetMapping("/{id}/roles")
    public List<String> getRoles(@PathVariable Long id) {
        return userUseCase.findUserRoles(id).stream().map(Role::getName).toList();
    }

    public record ResetAccessRequest(String password) {}

    @Operation(summary = "(Ré)initialiser l'accès d'un compte : renvoie l'identifiant et un mot de passe "
            + "temporaire (ou celui fourni) à changer à la première connexion")
    @PostMapping("/{id}/reset-access")
    @PreAuthorize("hasAuthority('user:update')")
    public UserUseCase.TemporaryAccess resetAccess(@PathVariable Long id,
                                                   @RequestBody(required = false) ResetAccessRequest req) {
        return userUseCase.resetAccess(id, req != null ? req.password() : null);
    }

    private UserResponse toResponse(gn.lacaeducation.iam.domain.User u) {
        return new UserResponse(u.getId(), u.getTenantId(), u.getFirstName(), u.getLastName(),
                u.getUsername(), u.getEmail(), u.getPhone(), u.getActive(), u.isMustChangePassword(),
                u.getPlainTemporaryPassword(), u.getLastLoginAt(), u.getCreatedAt());
    }

    record UpdateUserRequest(String firstName, String lastName, String phone, Boolean active) {}
}
