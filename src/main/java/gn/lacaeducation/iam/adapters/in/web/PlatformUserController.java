package gn.lacaeducation.iam.adapters.in.web;

import gn.lacaeducation.iam.adapters.in.web.dto.PlatformUserResponse;
import gn.lacaeducation.iam.application.port.in.UserUseCase;
import gn.lacaeducation.iam.application.port.out.RoleRepository;
import gn.lacaeducation.iam.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Platform Users", description = "Tous les utilisateurs, tous tenants confondus (supervision SUPER_ADMIN)")
@RestController
@RequestMapping("/api/v1/platform/users")
@RequiredArgsConstructor
public class PlatformUserController {

    /** Fenêtre au-delà de laquelle un utilisateur n'est plus considéré « en ligne ». */
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final UserUseCase userUseCase;
    private final RoleRepository roleRepository;

    @Operation(summary = "Lister tous les utilisateurs de tous les tenants, avec leurs rôles et leur statut en ligne/hors ligne")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PlatformUserResponse> findAll(@RequestParam(required = false) Long tenantId) {
        LocalDateTime cutoff = LocalDateTime.now().minus(ONLINE_WINDOW);
        List<User> users = tenantId != null ? userUseCase.findByTenantId(tenantId) : userUseCase.findAll();
        return users.stream().map(u -> toResponse(u, cutoff)).toList();
    }

    private PlatformUserResponse toResponse(User u, LocalDateTime cutoff) {
        List<String> roles = roleRepository.findUserRoles(u.getId()).stream()
                .map(r -> r.getCode() != null ? r.getCode().name() : r.getName())
                .toList();
        boolean online = u.getLastActivityAt() != null && u.getLastActivityAt().isAfter(cutoff);
        return new PlatformUserResponse(u.getId(), u.getTenantId(), u.getFirstName(), u.getLastName(),
                u.getUsername(), u.getEmail(), u.getPhone(), u.getActive(), roles,
                u.getLastLoginAt(), u.getLastActivityAt(), online);
    }
}
