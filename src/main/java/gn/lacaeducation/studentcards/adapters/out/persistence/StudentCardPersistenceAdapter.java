package gn.lacaeducation.studentcards.adapters.out.persistence;
import gn.lacaeducation.studentcards.adapters.out.persistence.mapper.StudentCardsMapper;
import gn.lacaeducation.studentcards.application.port.out.StudentCardRepository;
import gn.lacaeducation.studentcards.domain.CardStatus;
import gn.lacaeducation.studentcards.domain.StudentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class StudentCardPersistenceAdapter implements StudentCardRepository {
    private final StudentCardJpaRepository jpa;
    private final StudentCardsMapper mapper;
    @Override public StudentCard save(StudentCard c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<StudentCard> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<StudentCard> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<StudentCard> findByStudentId(Long s) { return jpa.findByStudentId(s).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<StudentCard> findByStudentIdAndStatus(Long s, CardStatus st) { return jpa.findByStudentIdAndStatus(s, st).map(mapper::toDomain); }
    @Override public Optional<StudentCard> findByQrPayload(String p) { return jpa.findByQrPayload(p).map(mapper::toDomain); }
    @Override public long countByTenantId(Long t) { return jpa.countByTenantId(t); }
}
