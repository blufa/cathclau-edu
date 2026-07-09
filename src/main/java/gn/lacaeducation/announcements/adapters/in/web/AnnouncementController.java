package gn.lacaeducation.announcements.adapters.in.web;

import gn.lacaeducation.announcements.application.port.in.AnnouncementUseCase;
import gn.lacaeducation.announcements.domain.Announcement;
import gn.lacaeducation.announcements.domain.AnnouncementScope;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Announcements", description = "Annonces de l'établissement (§8) — gestion par le personnel")
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementUseCase useCase;

    public record CreateRequest(String title, String content, AnnouncementScope scope,
                                Long classId, Long studentId, String authorName) {}
    public record UpdateRequest(String title, String content) {}

    @Operation(summary = "Publier une annonce (générale, de classe ou d'élève)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @PostMapping
    public ResponseEntity<Announcement> create(@RequestBody CreateRequest r, @AuthenticationPrincipal Jwt jwt) {
        Announcement a = useCase.create(new AnnouncementUseCase.CreateAnnouncementCommand(
                TenantContext.get(), r.title(), r.content(), r.scope(), r.classId(), r.studentId(),
                currentUserId(jwt), r.authorName()));
        return ResponseEntity.created(URI.create("/api/v1/announcements/" + a.getId())).body(a);
    }

    @Operation(summary = "Lister les annonces du tenant courant")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @GetMapping
    public List<Announcement> list() {
        return useCase.findByTenantId(TenantContext.get());
    }

    @Operation(summary = "Détail d'une annonce")
    @GetMapping("/{id}")
    public ResponseEntity<Announcement> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Modifier une annonce")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER','SECRETARY')")
    @PutMapping("/{id}")
    public Announcement update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return useCase.update(id, new AnnouncementUseCase.UpdateAnnouncementCommand(r.title(), r.content()));
    }

    @Operation(summary = "Supprimer une annonce")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
