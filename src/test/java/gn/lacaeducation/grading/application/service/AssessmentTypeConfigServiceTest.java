package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ConflictException;
import gn.lacaeducation.grading.application.port.in.AssessmentTypeConfigUseCase.TypeCommand;
import gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository;
import gn.lacaeducation.grading.domain.AssessmentTypeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentTypeConfigServiceTest {

    @Mock AssessmentTypeConfigRepository repo;
    @InjectMocks AssessmentTypeConfigService service;

    @Test
    void create_normalizesCodeAndPersists() {
        when(repo.existsByTenantIdAndCode(1L, "CONTROLE_CONTINU")).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        AssessmentTypeConfig t = service.create(1L, new TypeCommand(
                " contrôle continu ", "Contrôle continu", new BigDecimal("2"), new BigDecimal("40"), "#2E7D32", true));

        assertThat(t.getCode()).isEqualTo("CONTROLE_CONTINU");
        assertThat(t.getDefaultCoefficient()).isEqualByComparingTo("2");
        assertThat(t.isActive()).isTrue();
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        when(repo.existsByTenantIdAndCode(1L, "DEVOIR")).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, new TypeCommand(
                "DEVOIR", "Devoir", BigDecimal.ONE, null, null, true)))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_blankLabel_throws() {
        assertThatThrownBy(() -> service.create(1L, new TypeCommand(
                "ORAL", "  ", BigDecimal.ONE, null, null, true)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_negativeCoefficient_throws() {
        assertThatThrownBy(() -> service.create(1L, new TypeCommand(
                "TP", "Travaux pratiques", new BigDecimal("-1"), null, null, true)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
