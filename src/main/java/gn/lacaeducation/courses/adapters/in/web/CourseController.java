package gn.lacaeducation.courses.adapters.in.web;

import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.courses.application.port.in.CourseUseCase;
import gn.lacaeducation.courses.domain.Course;
import gn.lacaeducation.courses.domain.CourseResource;
import gn.lacaeducation.courses.domain.CourseResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Catalogue de cours payants (vidéo/PDF) par classe (§16 B). La gestion (création, ressources,
 * publication) est réservée au personnel ; la consultation du catalogue publié est ouverte à tout
 * utilisateur du tenant (élèves/parents inclus) pour parcourir l'offre avant achat
 * (voir {@code CoursePurchaseController} pour l'achat et le contenu déverrouillé).
 */
@Tag(name = "Courses", description = "Catalogue de cours payants (vidéo/PDF) par classe")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseUseCase useCase;

    public record CreateRequest(Long classId, String subjectLabel, String title, String description,
                                BigDecimal price, String thumbnailUrl) {}
    public record UpdateRequest(String subjectLabel, String title, String description,
                                BigDecimal price, String thumbnailUrl) {}
    public record AddResourceRequest(CourseResourceType type, String title, String fileUrl,
                                     Integer sortOrder, Integer durationSeconds) {}

    @Operation(summary = "Créer un cours (frais d'achat généré automatiquement)")
    @PreAuthorize("hasAuthority('cours:manage')")
    @PostMapping
    public ResponseEntity<Course> create(@RequestBody CreateRequest r) {
        Course c = useCase.create(new CourseUseCase.CreateCourseCommand(
                TenantContext.get(), r.classId(), r.subjectLabel(), r.title(), r.description(),
                r.price(), r.thumbnailUrl()));
        return ResponseEntity.created(URI.create("/api/v1/courses/" + c.getId())).body(c);
    }

    @Operation(summary = "Catalogue des cours publiés du tenant courant (optionnellement filtré par classe)")
    @GetMapping
    public List<Course> list(@RequestParam(required = false) Long classId) {
        List<Course> published = useCase.findPublishedByTenantId(TenantContext.get());
        return classId == null ? published : published.stream().filter(c -> classId.equals(c.getClassId())).toList();
    }

    @Operation(summary = "Tous les cours du tenant, y compris brouillons (gestion)")
    @PreAuthorize("hasAuthority('cours:manage')")
    @GetMapping("/gestion")
    public List<Course> manage(@RequestParam(required = false) Long classId) {
        List<Course> all = useCase.findByTenantId(TenantContext.get());
        return classId == null ? all : all.stream().filter(c -> classId.equals(c.getClassId())).toList();
    }

    @Operation(summary = "Détail d'un cours")
    @GetMapping("/{id}")
    public ResponseEntity<Course> findById(@PathVariable Long id) {
        return useCase.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Modifier un cours")
    @PreAuthorize("hasAuthority('cours:manage')")
    @PutMapping("/{id}")
    public Course update(@PathVariable Long id, @RequestBody UpdateRequest r) {
        return useCase.update(id, new CourseUseCase.UpdateCourseCommand(
                r.subjectLabel(), r.title(), r.description(), r.price(), r.thumbnailUrl()));
    }

    @Operation(summary = "Publier un cours (le rend achetable)")
    @PreAuthorize("hasAuthority('cours:manage')")
    @PatchMapping("/{id}/publier")
    public Course publish(@PathVariable Long id) {
        return useCase.publish(id);
    }

    @Operation(summary = "Dépublier un cours")
    @PreAuthorize("hasAuthority('cours:manage')")
    @PatchMapping("/{id}/depublier")
    public Course unpublish(@PathVariable Long id) {
        return useCase.unpublish(id);
    }

    @Operation(summary = "Supprimer un cours (impossible s'il a déjà été acheté)")
    @PreAuthorize("hasAuthority('cours:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    @Operation(summary = "Ajouter une ressource (vidéo ou PDF) à un cours")
    @PreAuthorize("hasAuthority('cours:manage')")
    @PostMapping("/{id}/ressources")
    public CourseResource addResource(@PathVariable Long id, @RequestBody AddResourceRequest r) {
        return useCase.addResource(id, new CourseUseCase.AddResourceCommand(
                r.type(), r.title(), r.fileUrl(), r.sortOrder(), r.durationSeconds()));
    }

    @Operation(summary = "Lister les ressources d'un cours avec leur URL (gestion)")
    @PreAuthorize("hasAuthority('cours:manage')")
    @GetMapping("/{id}/ressources")
    public List<CourseResource> resources(@PathVariable Long id) {
        return useCase.findResourcesByCourseId(id);
    }

    @Operation(summary = "Supprimer une ressource")
    @PreAuthorize("hasAuthority('cours:manage')")
    @DeleteMapping("/ressources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Long resourceId) {
        useCase.deleteResource(resourceId);
    }
}
