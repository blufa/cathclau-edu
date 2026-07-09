package gn.lacaeducation.announcements.application.service;

import gn.lacaeducation.announcements.application.port.in.AnnouncementUseCase;
import gn.lacaeducation.announcements.application.port.out.AnnouncementRepository;
import gn.lacaeducation.announcements.domain.Announcement;
import gn.lacaeducation.announcements.domain.AnnouncementScope;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AnnouncementService implements AnnouncementUseCase {

    private final AnnouncementRepository repo;

    @Override
    public Announcement create(CreateAnnouncementCommand c) {
        if (c.title() == null || c.title().isBlank()) {
            throw new BusinessRuleException("Le titre de l'annonce est requis.");
        }
        AnnouncementScope scope = c.scope() != null ? c.scope() : AnnouncementScope.GENERAL;
        if (scope == AnnouncementScope.CLASS && c.classId() == null) {
            throw new BusinessRuleException("Une annonce de classe doit préciser la classe.");
        }
        if (scope == AnnouncementScope.STUDENT && c.studentId() == null) {
            throw new BusinessRuleException("Une annonce d'élève doit préciser l'élève.");
        }
        LocalDateTime now = LocalDateTime.now();
        return repo.save(Announcement.builder()
                .tenantId(c.tenantId()).title(c.title()).content(c.content()).scope(scope)
                .classId(scope == AnnouncementScope.CLASS ? c.classId() : null)
                .studentId(scope == AnnouncementScope.STUDENT ? c.studentId() : null)
                .authorId(c.authorId()).authorName(c.authorName())
                .publishedAt(now).createdAt(now)
                .build());
    }

    @Override
    public Announcement update(Long id, UpdateAnnouncementCommand c) {
        Announcement a = load(id);
        if (c.title() != null) a.setTitle(c.title());
        if (c.content() != null) a.setContent(c.content());
        return repo.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Announcement> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findByTenantId(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private Announcement load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }
}
