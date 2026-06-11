package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.application.port.in.RoleUseCase;
import gn.lacaeducation.iam.domain.Permission;
import gn.lacaeducation.iam.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Roles & Permissions", description = "RBAC: roles management and permission assignment")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;

    @Operation(summary = "List all roles")
    @GetMapping("/api/v1/roles")
    public List<RoleResponse> findAll() {
        return roleUseCase.findAll().stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/api/v1/roles/{id}")
    public RoleResponse findById(@PathVariable Long id) {
        return toResponse(roleUseCase.findById(id));
    }

    @Operation(summary = "Create a custom role")
    @PostMapping("/api/v1/roles")
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest req) {
        Role role = roleUseCase.create(new RoleUseCase.CreateRoleCommand(req.name(), req.description()));
        return ResponseEntity.created(URI.create("/api/v1/roles/" + role.getId()))
                .body(toResponse(role));
    }

    @Operation(summary = "Update a role (custom roles only)")
    @PutMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody CreateRoleRequest req) {
        return toResponse(roleUseCase.update(id, new RoleUseCase.UpdateRoleCommand(req.name(), req.description())));
    }

    @Operation(summary = "Delete a custom role (system roles cannot be deleted)")
    @DeleteMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roleUseCase.delete(id);
    }

    @Operation(summary = "Get permissions catalog (read-only)")
    @GetMapping("/api/v1/permissions")
    public List<PermissionResponse> findAllPermissions() {
        return roleUseCase.findAllPermissions().stream().map(this::toPermissionResponse).toList();
    }

    @Operation(summary = "Assign permissions to a role (replaces existing)")
    @PostMapping("/api/v1/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permissions')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPermissions(@PathVariable Long id,
                                   @Valid @RequestBody AssignPermissionsRequest req) {
        roleUseCase.assignPermissions(id, req.permissionIds());
    }

    @Operation(summary = "Remove a role from a user")
    @DeleteMapping("/api/v1/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('role:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        roleUseCase.removeRoleFromUser(userId, roleId);
    }

    // DTO records
    record CreateRoleRequest(@NotBlank String name, String description) {}

    record AssignPermissionsRequest(@NotNull List<Long> permissionIds) {}

    record RoleResponse(Long id, String name, String code, String description,
                        List<PermissionResponse> permissions) {}

    record PermissionResponse(Long id, String name, String code) {}

    private RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = roleUseCase.findPermissionsByRole(role.getId())
                .stream().map(this::toPermissionResponse).toList();
        return new RoleResponse(role.getId(), role.getName(),
                role.getCode() != null ? role.getCode().name() : null,
                role.getDescription(), permissions);
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getCode());
    }
}
