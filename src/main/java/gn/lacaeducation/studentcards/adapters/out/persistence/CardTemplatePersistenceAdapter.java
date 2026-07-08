package gn.lacaeducation.studentcards.adapters.out.persistence;
import gn.lacaeducation.studentcards.adapters.out.persistence.mapper.StudentCardsMapper;
import gn.lacaeducation.studentcards.application.port.out.CardTemplateRepository;
import gn.lacaeducation.studentcards.domain.CardTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class CardTemplatePersistenceAdapter implements CardTemplateRepository {
    private final CardTemplateJpaRepository jpa;
    private final StudentCardsMapper mapper;
    @Override public CardTemplate save(CardTemplate t) { return mapper.toDomain(jpa.save(mapper.toEntity(t))); }
    @Override public Optional<CardTemplate> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<CardTemplate> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
