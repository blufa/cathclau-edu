package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.AssessmentTypeConfigUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository;
import gn.lacaeducation.grading.domain.AssessmentTypeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssessmentTypeConfigService implements AssessmentTypeConfigUseCase {

    private final AssessmentTypeConfigRepository repo;

    @Override
    public AssessmentTypeConfig create(Long tenantId, TypeCommand c) {
        String code = normalizeCode(c.code());
        validate(c, code);
        if (repo.existsByTenantIdAndCode(tenantId, code)) {
            throw new ConflictException("Un type d'évaluation avec le code " + code + " existe déjà.");
        }
        return repo.save(AssessmentTypeConfig.builder()
                .tenantId(tenantId).code(code).label(c.label())
                .defaultCoefficient(c.defaultCoefficient()).weight(c.weight()).color(c.color())
                .active(c.active() == null || c.active())
                .build());
    }

    @Override
    public AssessmentTypeConfig update(Long id, TypeCommand c) {
        AssessmentTypeConfig existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssessmentTypeConfig", id));
        // Le code reste stable (identité) ; on met à jour les attributs.
        validate(c, existing.getCode());
        existing.setLabel(c.label());
        existing.setDefaultCoefficient(c.defaultCoefficient());
        existing.setWeight(c.weight());
        existing.setColor(c.color());
        if (c.active() != null) existing.setActive(c.active());
        return repo.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentTypeConfig> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssessmentTypeConfig> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void validate(TypeCommand c, String code) {
        if (code == null || code.isBlank()) throw new BusinessRuleException("Le code du type est requis.");
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé du type est requis.");
        if (c.defaultCoefficient() != null && c.defaultCoefficient().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Le coefficient par défaut ne peut pas être négatif.");
        }
    }

    private String normalizeCode(String code) {
        if (code == null) return null;
        // Code technique : sans accents, en majuscules, espaces → underscore.
        String ascii = java.text.Normalizer.normalize(code.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }
}
