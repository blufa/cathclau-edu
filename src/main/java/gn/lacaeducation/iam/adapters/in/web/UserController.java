package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.common.PageResponse;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.iam.adapters.in.web.dto.CreateUserRequest;
import gn.lacaeducation.iam.adapters.in.web.dto.UserResponse;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
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

@Tag(name = "Users", description = "Gestion des utilisateurs au sein d'un tenant")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @Operation(summary = "Créer un nouvel utilisateur dans le tenant courant")
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

    @Operation(summary = "Lister tous les utilisateurs du tenant courant")
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PageResponse<UserResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        // Bug corrigé : lisait le claim JWT "tenant_id" directement, qui n'existe jamais pour un
        // SUPER_ADMIN (hors tenant) — même en impersonation via le header X-Tenant-Id (qui alimente
        // TenantContext, pas le JWT), la requête tombait sur tenantId=null et ne retournait jamais
        // aucun utilisateur. TenantContext.get() reflète correctement l'impersonation (voir
        // TenantContextFilter) — même pattern que StudentController.findAll.
        Long tenantId = TenantContext.get();
        List<UserResponse> list = userUseCase.findByTenantId(tenantId).stream().map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return PageResponse.of(new PageImpl<>(list.subList(start, end), pageable, list.size()));
    }

    @Operation(summary = "Consulter un utilisateur par ID")
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

    @Operation(summary = "Consulter le profil de l'utilisateur connecté")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return userUseCase.findById(userId).map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Mise à jour self-service du profil de l'utilisateur connecté (prénom, nom, "
            + "téléphone) — pour les rôles sans fiche métier dédiée (parent/élève/enseignant ont leur "
            + "propre self-service). Jamais l'e-mail ni l'état actif/inactif.")
    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateOwnProfileRequest req) {
        Long userId = Long.parseLong(jwt.getSubject());
        return toResponse(userUseCase.update(userId, new UserUseCase.UpdateUserCommand(
                req.firstName(), req.lastName(), req.phone(), null, null)));
    }

    @Operation(summary = "Mise à jour de la photo de profil de l'utilisateur connecté")
    @PatchMapping("/me/photo")
    public UserResponse updateMyPhoto(@AuthenticationPrincipal Jwt jwt, @RequestParam String photoUrl) {
        Long userId = Long.parseLong(jwt.getSubject());
        return toResponse(userUseCase.update(userId, new UserUseCase.UpdateUserCommand(null, null, null, null, photoUrl)));
    }

    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return toResponse(userUseCase.update(id, new UserUseCase.UpdateUserCommand(
                req.firstName(), req.lastName(), req.phone(), req.active(), null)));
    }

    @Operation(summary = "Désactiver un compte utilisateur")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        userUseCase.update(id, new UserUseCase.UpdateUserCommand(null, null, null, false, null));
    }

    @Operation(summary = "Réactiver un compte utilisateur")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('user:update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable Long id) {
        userUseCase.update(id, new UserUseCase.UpdateUserCommand(null, null, null, true, null));
    }

    @Operation(summary = "Supprimer (suppression logique) un utilisateur")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { userUseCase.delete(id); }

    @Operation(summary = "Affecter un rôle à un utilisateur")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable Long id, @RequestParam Long roleId) {
        userUseCase.assignRole(id, roleId);
    }

    public record UserRoleResponse(Long id, String name) {}

    @Operation(summary = "Consulter les rôles affectés à un utilisateur")
    @GetMapping("/{id}/roles")
    public List<UserRoleResponse> getRoles(@PathVariable Long id) {
        // Comparé par id côté frontend (pas par nom) : un rôle personnalisé peut porter le même
        // libellé qu'un rôle système (ex. deux rôles nommés « Comptable ») — comparer par nom faisait
        // alors apparaître le mauvais rôle comme attribué, rendant son retrait impossible en pratique.
        return userUseCase.findUserRoles(id).stream().map(r -> new UserRoleResponse(r.getId(), r.getName())).toList();
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
                u.getUsername(), u.getEmail(), u.getPhone(), u.getPhoto(), u.getActive(), u.isMustChangePassword(),
                u.getPlainTemporaryPassword(), u.getLastLoginAt(), u.getLastActivityAt(), u.getCreatedAt());
    }

    record UpdateUserRequest(String firstName, String lastName, String phone, Boolean active) {}

    record UpdateOwnProfileRequest(String firstName, String lastName, String phone) {}
}
