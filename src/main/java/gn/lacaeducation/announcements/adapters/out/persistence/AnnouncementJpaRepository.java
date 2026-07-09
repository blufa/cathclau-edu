package gn.lacaeducation.announcements.adapters.out.persistence;

import gn.lacaeducation.announcements.domain.AnnouncementScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface AnnouncementJpaRepository extends JpaRepository<AnnouncementJpaEntity, Long> {
    List<AnnouncementJpaEntity> findByTenantIdOrderByPublishedAtDesc(Long tenantId);

    @Query("SELECT a FROM AnnouncementJpaEntity a WHERE a.tenantId = :tenantId AND ("
            + "a.scope = gn.lacaeducation.announcements.domain.AnnouncementScope.GENERAL "
            + "OR (a.scope = gn.lacaeducation.announcements.domain.AnnouncementScope.CLASS AND a.classId IN :classIds) "
            + "OR (a.scope = gn.lacaeducation.announcements.domain.AnnouncementScope.STUDENT AND a.studentId IN :studentIds)"
            + ") ORDER BY a.publishedAt DESC")
    List<AnnouncementJpaEntity> findForFeed(@Param("tenantId") Long tenantId,
                                            @Param("classIds") Collection<Long> classIds,
                                            @Param("studentIds") Collection<Long> studentIds);
}
