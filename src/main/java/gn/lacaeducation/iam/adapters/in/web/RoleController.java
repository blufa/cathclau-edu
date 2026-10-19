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

@Tag(name = "Roles & Permissions", description = "RBAC : gestion des rôles et affectation des permissions")
@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;

    @Operation(summary = "Lister tous les rôles")
    @GetMapping("/api/v1/roles")
    public List<RoleResponse> findAll() {
        return roleUseCase.findAll().stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Consulter un rôle par ID")
    @GetMapping("/api/v1/roles/{id}")
    public RoleResponse findById(@PathVariable Long id) {
        return toResponse(roleUseCase.findById(id));
    }

    @Operation(summary = "Créer un rôle personnalisé")
    @PostMapping("/api/v1/roles")
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest req) {
        Role role = roleUseCase.create(new RoleUseCase.CreateRoleCommand(req.name(), req.description()));
        return ResponseEntity.created(URI.create("/api/v1/roles/" + role.getId()))
                .body(toResponse(role));
    }

    @Operation(summary = "Modifier un rôle (rôles personnalisés uniquement)")
    @PutMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody CreateRoleRequest req) {
        return toResponse(roleUseCase.update(id, new RoleUseCase.UpdateRoleCommand(req.name(), req.description())));
    }

    @Operation(summary = "Supprimer un rôle personnalisé (les rôles système ne peuvent pas être supprimés)")
    @DeleteMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roleUseCase.delete(id);
    }

    @Operation(summary = "Consulter le catalogue des permissions (lecture seule)")
    @GetMapping("/api/v1/permissions")
    public List<PermissionResponse> findAllPermissions() {
        return roleUseCase.findAllPermissions().stream().map(this::toPermissionResponse).toList();
    }

    @Operation(summary = "Affecter des permissions à un rôle (remplace les permissions existantes)")
    @PostMapping("/api/v1/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permissions')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPermissions(@PathVariable Long id,
                                   @Valid @RequestBody AssignPermissionsRequest req) {
        roleUseCase.assignPermissions(id, req.permissionIds());
    }

    @Operation(summary = "Retirer un rôle à un utilisateur")
    @DeleteMapping("/api/v1/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('role:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        roleUseCase.removeRoleFromUser(userId, roleId);
    }

    @Operation(summary = "Consulter les permissions accordées directement à un utilisateur (au-delà de son rôle)")
    @GetMapping("/api/v1/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permissions')")
    public List<PermissionResponse> findDirectPermissionsByUser(@PathVariable Long userId) {
        return roleUseCase.findDirectPermissionsByUser(userId).stream().map(this::toPermissionResponse).toList();
    }

    @Operation(summary = "Affecter des permissions supplémentaires directement à un utilisateur, en plus de son rôle (remplace les permissions directes existantes)")
    @PostMapping("/api/v1/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permissions')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignUserPermissions(@PathVariable Long userId, @Valid @RequestBody AssignPermissionsRequest req) {
        roleUseCase.assignUserPermissions(userId, req.permissionIds());
    }

    @Operation(summary = "Consulter les permissions retirées à un utilisateur précis (même si son rôle les accorde)")
    @GetMapping("/api/v1/users/{userId}/revoked-permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PermissionResponse> findRevokedPermissionsByUser(@PathVariable Long userId) {
        return roleUseCase.findRevokedPermissionsByUser(userId).stream().map(this::toPermissionResponse).toList();
    }

    @Operation(summary = "Réduire les privilèges d'un admin d'école précis — retire ces permissions à CET "
            + "utilisateur même si son rôle (partagé par toute la plateforme) les accorde ; réservé au "
            + "SUPER_ADMIN pour ne jamais affecter les autres écoles ni être défait par l'intéressé lui-même")
    @PostMapping("/api/v1/users/{userId}/revoked-permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeUserPermissions(@PathVariable Long userId, @Valid @RequestBody AssignPermissionsRequest req) {
        roleUseCase.revokeUserPermissions(userId, req.permissionIds());
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
