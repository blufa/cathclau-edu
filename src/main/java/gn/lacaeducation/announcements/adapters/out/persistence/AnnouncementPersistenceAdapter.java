package gn.lacaeducation.announcements.adapters.out.persistence;

import gn.lacaeducation.announcements.application.port.out.AnnouncementRepository;
import gn.lacaeducation.announcements.domain.Announcement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AnnouncementPersistenceAdapter implements AnnouncementRepository {

    /** Sentinelle pour éviter un {@code IN ()} vide (aucun identifiant réel ne vaut -1). */
    private static final List<Long> NONE = List.of(-1L);

    private final AnnouncementJpaRepository jpa;

    @Override
    public Announcement save(Announcement a) {
        return toDomain(jpa.save(toEntity(a)));
    }

    @Override
    public Optional<Announcement> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Announcement> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByPublishedAtDesc(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Announcement> findForFeed(Long tenantId, Collection<Long> classIds, Collection<Long> studentIds) {
        Collection<Long> classes = classIds == null || classIds.isEmpty() ? NONE : classIds;
        Collection<Long> students = studentIds == null || studentIds.isEmpty() ? NONE : studentIds;
        return jpa.findForFeed(tenantId, classes, students).stream().map(this::toDomain).toList();
    }

    private Announcement toDomain(AnnouncementJpaEntity e) {
        return Announcement.builder()
                .id(e.getId()).tenantId(e.getTenantId()).title(e.getTitle()).content(e.getContent())
                .scope(e.getScope()).classId(e.getClassId()).studentId(e.getStudentId())
                .authorId(e.getAuthorId()).authorName(e.getAuthorName())
                .publishedAt(e.getPublishedAt()).createdAt(e.getCreatedAt())
                .build();
    }

    private AnnouncementJpaEntity toEntity(Announcement d) {
        AnnouncementJpaEntity e = new AnnouncementJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setTitle(d.getTitle()); e.setContent(d.getContent());
        e.setScope(d.getScope()); e.setClassId(d.getClassId()); e.setStudentId(d.getStudentId());
        e.setAuthorId(d.getAuthorId()); e.setAuthorName(d.getAuthorName());
        e.setPublishedAt(d.getPublishedAt()); e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
