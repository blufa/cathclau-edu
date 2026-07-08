package gn.lacaeducation.announcements;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** API publique du module annonces : feed lisible par un parent (portail, §8). */
public interface AnnouncementFeedProvider {
    record AnnouncementView(Long id, String title, String content, String scope,
                            String authorName, LocalDateTime publishedAt) {}

    /**
     * Annonces visibles par le parent : générales + celles des classes de ses enfants + celles de
     * ses enfants. Triées de la plus récente à la plus ancienne.
     */
    List<AnnouncementView> findForParent(Long tenantId, Collection<Long> classIds, Collection<Long> studentIds);
}
