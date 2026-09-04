package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.PlatformFeeQuoteApi;
import gn.lacaeducation.platform.application.port.in.PlatformFeeSettingUseCase;
import gn.lacaeducation.platform.application.port.out.PlatformFeeSettingRepository;
import gn.lacaeducation.platform.domain.PlatformFeeSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Montant du frais d'usage plateforme (SUPER_ADMIN). Implémente à la fois le port d'entrée
 * (administration) et le {@link PlatformFeeQuoteApi} publié consommé par {@code billing} — un seul
 * point de vérité pour « quel est le montant actif ».
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformFeeSettingService implements PlatformFeeSettingUseCase, PlatformFeeQuoteApi {

    private final PlatformFeeSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal currentAmount() {
        return repository.findActive().map(PlatformFeeSetting::getAmount).orElse(BigDecimal.ZERO);
    }

    @Override
    public PlatformFeeSetting setAmount(BigDecimal amount, Long createdByUserId) {
        if (amount == null || amount.signum() < 0) {
            throw new gn.lacaeducation.common.BusinessRuleException(
                    "Le montant du frais plateforme ne peut pas être négatif");
        }
        repository.deactivateActive();
        return repository.save(PlatformFeeSetting.builder()
                .amount(amount)
                .active(true)
                .createdByUserId(createdByUserId)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
