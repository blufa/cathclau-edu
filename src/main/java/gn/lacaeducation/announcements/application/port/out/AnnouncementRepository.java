package gn.lacaeducation.announcements.application.port.out;

import gn.lacaeducation.announcements.domain.Announcement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository {
    Announcement save(Announcement announcement);
    Optional<Announcement> findById(Long id);
    List<Announcement> findByTenantId(Long tenantId);
    void deleteById(Long id);

    /**
     * Annonces visibles par un parent : générales (toute l'école), celles des classes {@code classIds}
     * et celles des élèves {@code studentIds}. Triées de la plus récente à la plus ancienne.
     */
    List<Announcement> findForFeed(Long tenantId, Collection<Long> classIds, Collection<Long> studentIds);
}
