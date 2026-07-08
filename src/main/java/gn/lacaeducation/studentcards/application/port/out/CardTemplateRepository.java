package gn.lacaeducation.studentcards.application.port.out;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import java.util.List;
import java.util.Optional;
public interface CardTemplateRepository {
    CardTemplate save(CardTemplate t);
    Optional<CardTemplate> findById(Long id);
    List<CardTemplate> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
