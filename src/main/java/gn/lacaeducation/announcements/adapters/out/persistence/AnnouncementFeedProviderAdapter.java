package gn.lacaeducation.announcements.adapters.out.persistence;

import gn.lacaeducation.announcements.AnnouncementFeedProvider;
import gn.lacaeducation.announcements.application.port.out.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
class AnnouncementFeedProviderAdapter implements AnnouncementFeedProvider {

    private final AnnouncementRepository repo;

    @Override
    public List<AnnouncementView> findForParent(Long tenantId, Collection<Long> classIds, Collection<Long> studentIds) {
        return repo.findForFeed(tenantId, classIds, studentIds).stream()
                .map(a -> new AnnouncementView(a.getId(), a.getTitle(), a.getContent(),
                        a.getScope() != null ? a.getScope().name() : null, a.getAuthorName(), a.getPublishedAt()))
                .toList();
    }
}
