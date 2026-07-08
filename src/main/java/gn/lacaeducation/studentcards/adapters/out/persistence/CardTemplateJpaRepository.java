package gn.lacaeducation.studentcards.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface CardTemplateJpaRepository extends JpaRepository<CardTemplateJpaEntity, Long> {
    List<CardTemplateJpaEntity> findByTenantId(Long tenantId);
}
