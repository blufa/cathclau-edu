package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.EmailVerificationTokenRepository;
import gn.lacaeducation.platform.domain.EmailVerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class EmailVerificationTokenPersistenceAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }
}
