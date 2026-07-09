package gn.lacaeducation.portal.adapters.in.web;

import gn.lacaeducation.announcements.AnnouncementFeedProvider;
import gn.lacaeducation.announcements.AnnouncementFeedProvider.AnnouncementView;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.ParentAccessProvider;
import gn.lacaeducation.students.StudentDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feed d'annonces du portail parent (§8) : annonces générales de l'école + annonces des classes des
 * enfants + annonces concernant ses enfants. Lecture seule, scopé au parent connecté.
 */
@Tag(name = "Parent Portal", description = "Annonces visibles par le parent connecté")
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
public class ParentAnnouncementsController {

    private final ParentAccessProvider parentAccessProvider;
    private final StudentDataProvider studentDataProvider;
    private final AnnouncementFeedProvider announcementFeedProvider;

    @Operation(summary = "Annonces visibles par le parent : générales, des classes de ses enfants et de ses enfants")
    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/announcements")
    public List<AnnouncementView> announcements(@AuthenticationPrincipal Jwt jwt) {
        List<Long> studentIds = parentAccessProvider.findChildStudentIds(currentUserId(jwt));
        List<Long> classIds = studentIds.stream()
                .map(studentDataProvider::findCurrentClassId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .distinct()
                .toList();
        return announcementFeedProvider.findForParent(TenantContext.get(), classIds, studentIds);
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null) return null;
        try { return Long.parseLong(jwt.getSubject()); } catch (RuntimeException e) { return null; }
    }
}
