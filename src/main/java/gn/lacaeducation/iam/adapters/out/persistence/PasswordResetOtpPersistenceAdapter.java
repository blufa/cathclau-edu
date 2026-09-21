package gn.lacaeducation.iam.adapters.out.persistence;

import gn.lacaeducation.iam.application.port.out.PasswordResetOtpRepository;
import gn.lacaeducation.iam.domain.PasswordResetOtp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PasswordResetOtpPersistenceAdapter implements PasswordResetOtpRepository {

    private final PasswordResetOtpJpaRepository jpaRepository;

    @Override
    public PasswordResetOtp save(PasswordResetOtp otp) {
        return toDomain(jpaRepository.save(toEntity(otp)));
    }

    @Override
    public Optional<PasswordResetOtp> findByUserIdAndCode(Long userId, String code) {
        return jpaRepository.findByUserIdAndCode(userId, code).map(this::toDomain);
    }

    private PasswordResetOtp toDomain(PasswordResetOtpJpaEntity e) {
        return PasswordResetOtp.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .code(e.getCode())
                .expiresAt(e.getExpiresAt())
                .used(e.isUsed())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PasswordResetOtpJpaEntity toEntity(PasswordResetOtp d) {
        PasswordResetOtpJpaEntity e = new PasswordResetOtpJpaEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setCode(d.getCode());
        e.setExpiresAt(d.getExpiresAt());
        e.setUsed(d.isUsed());
        return e;
    }
}
