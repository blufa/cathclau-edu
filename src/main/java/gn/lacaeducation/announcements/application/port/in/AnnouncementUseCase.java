package gn.lacaeducation.announcements.application.port.in;

import gn.lacaeducation.announcements.domain.Announcement;
import gn.lacaeducation.announcements.domain.AnnouncementScope;

import java.util.List;
import java.util.Optional;

public interface AnnouncementUseCase {
    record CreateAnnouncementCommand(Long tenantId, String title, String content, AnnouncementScope scope,
                                     Long classId, Long studentId, Long authorId, String authorName) {}
    record UpdateAnnouncementCommand(String title, String content) {}

    Announcement create(CreateAnnouncementCommand command);
    Announcement update(Long id, UpdateAnnouncementCommand command);
    Optional<Announcement> findById(Long id);
    List<Announcement> findByTenantId(Long tenantId);
    void delete(Long id);
}
